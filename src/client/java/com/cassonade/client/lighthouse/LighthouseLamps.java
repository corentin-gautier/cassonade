package com.cassonade.client.lighthouse;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.cassonade.lighthouse.LighthousesPayload;

/**
 * A lighthouse's lamp seen from far away at night: a square of light at its top, the same few pixels wide at any
 * distance, seen only as its turning beam sweeps past the player: a pulse now and then out in the dark, the way a real
 * one is told from a star.
 * The server says where the lamps are, even past the terrain drawn; near the lighthouse, the lamp itself takes over
 * and the halo fades out. Drawn without fog, as a light in the dark carries farther than the land around it.
 */
public final class LighthouseLamps {
	/** Under this, the sea lantern alone is seen; the halo comes in over the next stretch. */
	private static final double NEAR = 48;
	private static final double FADE = 48;
	/** The halo's half width and its glow's, as a share of its distance: a few pixels on screen. */
	private static final float CORE = 0.005F;
	private static final float GLOW = 0.011F;
	/** The beam's turn, in ticks, and how narrow its flash is. */
	private static final float TURN = 100;
	private static final float SHARPNESS = 8;
	private static final int CORE_COLOR = 0xFFF4C8;
	private static final int GLOW_COLOR = 0xFFC860;
	/** How dark the sky gets at the dead of night, as the ambience reads it. */
	private static final float DARKEST_SKY = 11;
	private static List<BlockPos> lamps = List.of();
	private static @Nullable ClientLevel shownIn;

	private LighthouseLamps() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(LighthousesPayload.TYPE, (payload, context) -> {
			lamps = List.copyOf(payload.lamps());
			shownIn = context.client().level;
		});
		LevelRenderEvents.COLLECT_SUBMITS.register(LighthouseLamps::draw);
	}

	/** The lamps the client knows, for tests. */
	public static List<BlockPos> lamps() {
		return lamps;
	}

	private static void draw(LevelRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || level != shownIn || lamps.isEmpty()) {
			return;
		}
		float night = Mth.clamp(level.getSkyDarken() / DARKEST_SKY, 0, 1);
		if (night <= 0) {
			return;
		}
		float time = level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 camera = context.levelState().cameraRenderState.pos;
		// Within the depth drawn, so the far plane never clips it; the terrain in front still hides it
		double farthest = context.levelState().cameraRenderState.depthFar * 0.9;
		PoseStack poseStack = context.poseStack();
		context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
			for (BlockPos lamp : lamps) {
				draw(pose, buffer, lamp, camera, farthest, night, time);
			}
		});
	}

	private static void draw(PoseStack.Pose pose, VertexConsumer buffer, BlockPos lamp, Vec3 camera, double farthest, float night, float time) {
		Vec3 toward = Vec3.atCenterOf(lamp).subtract(camera);
		double distance = toward.length();
		float near = (float) Mth.clamp((distance - NEAR) / FADE, 0, 1);
		if (near <= 0) {
			return;
		}
		// The beam turns at its own pace from each lighthouse, and flashes when it points at the player
		float phase = (Mth.murmurHash3Mixer((int) lamp.asLong()) & 0xFFFF) / 65536F * Mth.TWO_PI;
		float beam = time / TURN * Mth.TWO_PI + phase;
		float facing = (float) Mth.atan2(-toward.z, -toward.x);
		float flash = (float) Math.pow(Math.max(0, Mth.cos(beam - facing)), SHARPNESS);
		float strength = night * near * flash;
		if (strength < 0.02F) {
			return;
		}
		Vec3 at = distance > farthest ? toward.scale(farthest / distance) : toward;
		double shown = at.length();
		Vec3 right = at.cross(new Vec3(0, 1, 0)).normalize();
		Vec3 up = right.cross(at).normalize();
		square(pose, buffer, at, right, up, (float) shown * GLOW * (0.5F + 0.5F * flash), ARGB.color(Mth.floor(160 * strength), GLOW_COLOR));
		square(pose, buffer, at, right, up, (float) shown * CORE * (0.6F + 0.4F * flash), ARGB.color(Mth.floor(255 * strength), CORE_COLOR));
	}

	private static void square(PoseStack.Pose pose, VertexConsumer buffer, Vec3 center, Vec3 right, Vec3 up, float half, int color) {
		Vec3 r = right.scale(half);
		Vec3 u = up.scale(half);
		vertex(pose, buffer, center.subtract(r).add(u), color);
		vertex(pose, buffer, center.add(r).add(u), color);
		vertex(pose, buffer, center.add(r).subtract(u), color);
		vertex(pose, buffer, center.subtract(r).subtract(u), color);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 at, int color) {
		buffer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(color);
	}
}
