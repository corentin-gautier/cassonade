package com.cassonade.client.ambience;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Every other clear night over snowy land, curtains of light to the north: blocky columns that sway and swell, green below
 * and violet above. They stay at the same distance around the camera, as the moon does, and the terrain hides them.
 */
public final class Aurora {
	private static final float RADIUS = 300;
	private static final float BASE = 70;
	private static final float MIN_HEIGHT = 50;
	private static final float MAX_HEIGHT = 130;
	/** Their reach to each side of north. */
	private static final float ARC = 75 * Mth.DEG_TO_RAD;
	private static final int COLUMNS = 72;
	private static final int NIGHT_FROM = 13000;
	private static final int NIGHT_UNTIL = 23000;
	private static final float EASE = 0.01F;
	private static final int GREEN = 0x4CFF9A;
	private static final int VIOLET = 0x8A5CFF;
	private static final float MAX_ALPHA = 0.42F;
	private static float strength;

	private Aurora() {
	}

	static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null) {
			strength = 0;
			return;
		}
		if (!minecraft.isPaused()) {
			strength = Mth.approach(strength, canShow(level, minecraft.player.blockPosition()) ? 1 : 0, EASE);
		}
	}

	private static boolean canShow(ClientLevel level, BlockPos pos) {
		long tick = Ambience.tickOfDay(level);
		long night = Math.floorDiv(level.getDefaultClockTime(), Ambience.TICKS_PER_DAY);
		return Ambience.overworld(level) && night % 2 != 0 && tick >= NIGHT_FROM && tick < NIGHT_UNTIL && !level.isRaining()
				&& level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel());
	}

	public static float strength() {
		return strength;
	}

	static void draw(LevelRenderContext context) {
		if (strength <= 0) {
			return;
		}
		float time = (Minecraft.getInstance().level.getGameTime() % 100000L) + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		float shown = strength;
		PoseStack poseStack = context.poseStack();
		context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
			for (int i = 0; i < COLUMNS; i++) {
				column(pose, buffer, i, time, shown);
			}
		});
	}

	private static void column(PoseStack.Pose pose, VertexConsumer buffer, int i, float time, float shown) {
		float swell = wave(i * 0.21F, time * 0.012F) * 0.6F + wave(i * 0.53F + 4, time * -0.02F) * 0.4F;
		float across = Mth.clamp(1 - Math.abs(i - COLUMNS / 2F) / (COLUMNS / 2F), 0, 1);
		float alpha = MAX_ALPHA * shown * swell * Mth.sqrt(across);
		float height = Mth.lerp(swell, MIN_HEIGHT, MAX_HEIGHT);
		float left = Mth.lerp(i / (float) COLUMNS, -ARC, ARC);
		float right = Mth.lerp((i + 1) / (float) COLUMNS, -ARC, ARC);
		// Each edge sways on its own, shared with the next column so no gap opens between them
		float x0 = Mth.sin(left) * RADIUS;
		float z0 = -Mth.cos(left) * RADIUS + sway(i, time);
		float x1 = Mth.sin(right) * RADIUS;
		float z1 = -Mth.cos(right) * RADIUS + sway(i + 1, time);
		float brightest = BASE + height * 0.18F;
		float middle = BASE + height * 0.5F;
		int under = ARGB.color(0, GREEN);
		int bottom = ARGB.color(Mth.floor(alpha * 255), GREEN);
		int fading = ARGB.color(Mth.floor(alpha * 120), VIOLET);
		int top = ARGB.color(0, VIOLET);
		quad(pose, buffer, x0, z0, x1, z1, BASE, under, brightest, bottom);
		quad(pose, buffer, x0, z0, x1, z1, brightest, bottom, middle, fading);
		quad(pose, buffer, x0, z0, x1, z1, middle, fading, BASE + height, top);
	}

	private static float sway(int edge, float time) {
		return Mth.sin(edge * 0.15F + time * 0.008F) * 14;
	}

	/** Between 0 and 1. */
	private static float wave(float place, float time) {
		return Mth.sin(place + time) * 0.5F + 0.5F;
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float x0, float z0, float x1, float z1, float y0, int color0, float y1, int color1) {
		buffer.addVertex(pose, x0, y0, z0).setColor(color0);
		buffer.addVertex(pose, x1, y0, z1).setColor(color0);
		buffer.addVertex(pose, x1, y1, z1).setColor(color1);
		buffer.addVertex(pose, x0, y1, z0).setColor(color1);
	}
}
