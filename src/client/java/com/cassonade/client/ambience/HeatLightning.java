package com.cassonade.client.ambience;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

/**
 * Some warm evenings, with no rain anywhere near, silent glows flicker low on the horizon, from a storm too far off to hear.
 * They stay at the same distance around the camera, as the moon does, and the terrain hides them.
 */
public final class HeatLightning {
	private static final float RADIUS = 300;
	private static final float BASE = -15;
	private static final float MIN_HEIGHT = 30;
	private static final float MAX_HEIGHT = 55;
	private static final float ARC = 30 * Mth.DEG_TO_RAD;
	private static final int COLUMNS = 40;
	private static final int EVENING_FROM = 12000;
	private static final int EVENING_UNTIL = 15000;
	/** One evening in this many. */
	private static final int ONE_EVENING_IN = 4;
	private static final float MIN_TEMPERATURE = 0.7F;
	private static final int MIN_WAIT = 60;
	private static final int MAX_WAIT = 320;
	/** How soon to look again when it is not such an evening. */
	private static final int RETRY = 100;
	private static final int LIFE = 14;
	private static final float MAX_ALPHA = 0.32F;
	private static final int COLOR = 0xFFE6D2;
	private static final RandomSource RANDOM = RandomSource.create();
	private static int wait;
	private static @Nullable Flash flash;

	private HeatLightning() {
	}

	static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null) {
			flash = null;
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		if (flash != null && ++flash.age >= LIFE) {
			flash = null;
		}
		if (--wait <= 0) {
			if (isEvening(level) && isWarm(level, minecraft.player.blockPosition())) {
				flash();
				wait = Mth.randomBetweenInclusive(RANDOM, MIN_WAIT, MAX_WAIT);
			} else {
				wait = RETRY;
			}
		}
	}

	/** The same evenings for everyone on a server, from the day's number. */
	public static boolean isEvening(ClientLevel level) {
		long tick = Ambience.tickOfDay(level);
		long day = Math.floorDiv(level.getDefaultClockTime(), Ambience.TICKS_PER_DAY);
		return Ambience.overworld(level) && tick >= EVENING_FROM && tick < EVENING_UNTIL && !level.isRaining()
				&& Math.floorMod(Mth.murmurHash3Mixer((int) day + 3), ONE_EVENING_IN) == 0;
	}

	public static boolean isWarm(ClientLevel level, BlockPos pos) {
		Biome biome = level.getBiome(pos).value();
		return biome.getBaseTemperature() >= MIN_TEMPERATURE && !biome.coldEnoughToSnow(pos, level.getSeaLevel());
	}

	private static void flash() {
		flash(RANDOM.nextFloat() * Mth.TWO_PI);
	}

	/** Around this bearing, 0 to the north; for tests, to look at it. */
	public static void flash(float bearing) {
		flash = new Flash(bearing, RANDOM.nextFloat() * 100);
	}

	public static boolean isFlashing() {
		return flash != null;
	}

	static void draw(LevelRenderContext context) {
		Flash shown = flash;
		if (shown == null) {
			return;
		}
		PoseStack poseStack = context.poseStack();
		context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> shown.draw(pose, buffer));
	}

	private static final class Flash {
		final float bearing;
		final float seed;
		int age;

		Flash(float bearing, float seed) {
			this.bearing = bearing;
			this.seed = seed;
		}

		/** A bright start, a dip, a second and weaker flicker, then nothing: whole ticks, as lightning is drawn. */
		private float brightness() {
			return switch (age) {
				case 0, 1 -> 1;
				case 2, 3 -> 0.35F;
				case 4, 5 -> 0.8F;
				case 6, 7 -> 0.5F;
				default -> 0.5F * (LIFE - age) / (LIFE - 8F);
			};
		}

		void draw(PoseStack.Pose pose, VertexConsumer buffer) {
			float brightness = brightness();
			for (int i = 0; i < COLUMNS; i++) {
				// Strongest in the middle, its top rolling slowly like a far cloud's: quick changes would read as towers
				float across = 1 - Math.abs(i + 0.5F - COLUMNS / 2F) / (COLUMNS / 2F);
				float height = Mth.lerp((Mth.sin(i * 0.18F + seed) + 1) / 2 * across, MIN_HEIGHT, MAX_HEIGHT);
				float left = bearing + Mth.lerp(i / (float) COLUMNS, -ARC, ARC);
				float right = bearing + Mth.lerp((i + 1) / (float) COLUMNS, -ARC, ARC);
				int bottom = ARGB.color(Mth.floor(255 * MAX_ALPHA * brightness * across), COLOR);
				int top = ARGB.color(0, COLOR);
				buffer.addVertex(pose, Mth.sin(left) * RADIUS, BASE, -Mth.cos(left) * RADIUS).setColor(bottom);
				buffer.addVertex(pose, Mth.sin(right) * RADIUS, BASE, -Mth.cos(right) * RADIUS).setColor(bottom);
				buffer.addVertex(pose, Mth.sin(right) * RADIUS, BASE + height, -Mth.cos(right) * RADIUS).setColor(top);
				buffer.addVertex(pose, Mth.sin(left) * RADIUS, BASE + height, -Mth.cos(left) * RADIUS).setColor(top);
			}
		}
	}
}
