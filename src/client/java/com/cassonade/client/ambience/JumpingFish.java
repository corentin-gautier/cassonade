package com.cassonade.client.ambience;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * At dawn and dusk, a fish leaps out of open water near the player, with a splash each way. Like the birds, nothing but a
 * picture and a sound on the client: the game's own cod, no entity, tilting along the leap with its tail beating.
 */
public final class JumpingFish {
	private static final int DAWN_FROM = 23000;
	private static final int DAWN_UNTIL = 1500;
	private static final int DUSK_FROM = 11000;
	private static final int DUSK_UNTIL = 13500;
	private static final int MIN_WAIT = 80;
	private static final int MAX_WAIT = 400;
	private static final int RETRY = 40;
	private static final int MIN_REACH = 6;
	private static final int MAX_REACH = 22;
	/** Places tried at each go: a pond is often only a part of what is around. */
	private static final int TRIES = 12;
	private static final int LIFE = 14;
	private static final float HEIGHT = 1.0F;
	private static final float REACH = 1.4F;
	/** Between the model's origin and the middle of its body, which the leap tilts around. */
	private static final float BODY_HEIGHT = 1.375F;
	private static final float MAX_TILT = 0.9F;
	private static final float TAIL_SPEED = 1.6F;
	private static final float TAIL_SWING = 0.6F;
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/cod.png");
	private static final float VOLUME = 0.5F;
	/** Beyond the farthest leap, which stays faintly heard. */
	private static final float SPLASH_RANGE = 30;
	private static final RandomSource RANDOM = RandomSource.create();
	private static int wait;
	private static @Nullable Leap leap;
	private static @Nullable ModelPart model;
	private static boolean held;

	private JumpingFish() {
	}

	static void tick(Minecraft minecraft, ClientLevel level) {
		if (leap != null && !held && ++leap.age >= LIFE) {
			splash(minecraft, level, leap.position(LIFE));
			leap = null;
		}
		if (--wait <= 0) {
			wait = canLeap(level) && release(minecraft, level) ? Mth.randomBetweenInclusive(RANDOM, MIN_WAIT, MAX_WAIT) : RETRY;
		}
	}

	public static boolean canLeap(ClientLevel level) {
		long tick = Ambience.tickOfDay(level);
		boolean dawn = tick >= DAWN_FROM || tick < DAWN_UNTIL;
		boolean dusk = tick >= DUSK_FROM && tick < DUSK_UNTIL;
		return Ambience.overworld(level) && (dawn || dusk) && !level.isRaining();
	}

	/** A leap from some open water around the player, if there is any. */
	public static boolean release(Minecraft minecraft, ClientLevel level) {
		BlockPos around = minecraft.player.blockPosition();
		for (int i = 0; i < TRIES; i++) {
			float angle = RANDOM.nextFloat() * Mth.TWO_PI;
			int distance = Mth.randomBetweenInclusive(RANDOM, MIN_REACH, MAX_REACH);
			int x = around.getX() + Mth.floor(Mth.cos(angle) * distance);
			int z = around.getZ() + Mth.floor(Mth.sin(angle) * distance);
			BlockPos surface = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
			// Deep enough for a fish, not a puddle or a bucket's spill
			if (level.getFluidState(surface).is(FluidTags.WATER) && level.getFluidState(surface.below()).is(FluidTags.WATER)) {
				float heading = RANDOM.nextFloat() * Mth.TWO_PI;
				leap = new Leap(new Vec3(x + 0.5, surface.getY() + 0.9, z + 0.5), Mth.cos(heading), Mth.sin(heading));
				splash(minecraft, level, leap.position(0));
				return true;
			}
		}
		return false;
	}

	private static void splash(Minecraft minecraft, ClientLevel level, Vec3 at) {
		for (int i = 0; i < 6; i++) {
			level.addParticle(ParticleTypes.SPLASH, at.x + RANDOM.nextFloat() * 0.5 - 0.25, at.y, at.z + RANDOM.nextFloat() * 0.5 - 0.25, 0, 0.1, 0);
		}
		AmbientSound.of(SoundEvents.FISH_SWIM.location(), VOLUME).from(at).fading(SPLASH_RANGE).pitch(Mth.randomBetween(RANDOM, 1.1F, 1.4F)).play(minecraft);
	}

	/** For tests: holds the fish at this moment of its leap, or lets it go on with a negative one. */
	public static void hold(int age) {
		held = age >= 0;
		if (leap != null && held) {
			leap.age = age;
		}
	}

	public static boolean isLeaping() {
		return leap != null;
	}

	/** Where the fish is now, in the world; null when none is out of the water. */
	public static @Nullable Vec3 position() {
		return leap == null ? null : leap.position(leap.age);
	}

	static void draw(LevelRenderContext context) {
		Leap shown = leap;
		if (shown == null) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		float time = shown.age + (held ? 0 : minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
		if (model == null) {
			model = minecraft.getEntityModels().bakeLayer(ModelLayers.COD);
		}
		model.getChild("tail_fin").yRot = Mth.sin(time * TAIL_SPEED) * TAIL_SWING;
		Vec3 at = shown.position(time);
		Vec3 camera = context.levelState().cameraRenderState.pos;
		PoseStack pose = context.poseStack();
		pose.pushPose();
		pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
		// The model's head is towards its -Z: heading along the leap, climbing and then diving
		pose.rotate(Axis.YP.rotation((float) Mth.atan2(-shown.headingX, -shown.headingZ)));
		pose.rotate(Axis.XP.rotation((1 - 2 * time / LIFE) * MAX_TILT));
		pose.scale(-1, -1, 1);
		pose.translate(0, -BODY_HEIGHT, 0);
		int light = LightCoordsUtil.pack(0, minecraft.level.getBrightness(LightLayer.SKY, BlockPos.containing(at)));
		context.submitNodeCollector().submitModelPart(model, pose, RenderTypes.entityCutout(TEXTURE), light, OverlayTexture.NO_OVERLAY, null, ARGB.opaque(0xFFFFFF));
		pose.popPose();
	}

	private static final class Leap {
		final Vec3 start;
		final float headingX;
		final float headingZ;
		int age;

		Leap(Vec3 start, float headingX, float headingZ) {
			this.start = start;
			this.headingX = headingX;
			this.headingZ = headingZ;
		}

		Vec3 position(float time) {
			float progress = time / LIFE;
			return start.add(headingX * REACH * progress, HEIGHT * 4 * progress * (1 - progress), headingZ * REACH * progress);
		}
	}
}
