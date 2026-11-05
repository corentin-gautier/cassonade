package com.cassonade.client.ambience;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Banks of glowing plankton in warm seas: on one night in three, patches of the sea a few dozen blocks across light up
 * wherever the water is stirred, as the real ones do. Calm, a bank only sparkles here and there; a wave breaking on the
 * shore (Foam), a boat's wake, a swimmer, a leaping fish or the rain light it up, an electric blue that dies down within
 * a second. Only a picture on the client, like the rest of the ambience.
 */
public final class Plankton {
	/** Banks are drawn on a grid of cells this wide, each holding one or none. */
	private static final int CELL = 48;
	private static final float BANK_CHANCE = 0.4F;
	private static final float MIN_RADIUS = 5;
	private static final float MAX_RADIUS = 20;
	/** Their edge wanders by cells this wide, so that no bank is a plain disc. */
	private static final int EDGE_CELL = 5;
	private static final int ONE_NIGHT_IN = 3;
	/** How dark the sky must be for the glow to show, as Foam reads it. */
	private static final float DARKEST_SKY = 11;
	private static final float DARK_ENOUGH = 0.5F;
	/** Calm water: a few sparkles a tick, looked for around the player; more under the rain. */
	private static final int REACH = 28;
	private static final int CALM_TRIES = 12;
	private static final int RAIN_TRIES = 48;
	private static final int BOAT_REACH = 48;
	private static final int MAX_SPARKS = 900;
	/** The surface of a full water block, just above it, as Foam lays its foam. */
	private static final float SURFACE = 0.9F;
	private static final float PIXEL = 1 / 16F;
	/** How far out to sea a beach's water looks for its sea, a beach being neither warm nor cold. */
	private static final int OFFSHORE = 8;
	static final int GLOW = ARGB.color(255, 70, 205, 255);
	/** What a spark dims to before it goes out, the deep blue of the dark water. */
	private static final int DIM = ARGB.color(255, 25, 70, 140);
	private static final RandomSource RANDOM = RandomSource.create();
	private static final List<Spark> SPARKS = new ArrayList<>();
	private static boolean everywhere;
	private static int age;

	private record Spark(float x, float y, float z, int pixels, int born, int life) {
	}

	private Plankton() {
	}

	/** For the dev command and the tests: tonight, every warm sea glows as one bank. */
	public static void setEverywhere(boolean on) {
		everywhere = on;
	}

	public static boolean everywhere() {
		return everywhere;
	}

	public static int sparks() {
		return SPARKS.size();
	}

	/** Whether the water here belongs to a bank, glowing tonight. */
	public static boolean glows(ClientLevel level, BlockPos water) {
		if (night(level) < DARK_ENOUGH) {
			return false;
		}
		long night = Math.floorDiv(level.getDefaultClockTime() - Ambience.TICKS_PER_DAY / 2, Ambience.TICKS_PER_DAY);
		if (!everywhere && (Math.floorMod(mix(night * 0x51ED27L), ONE_NIGHT_IN) != 0 || !inBank(water.getX(), water.getZ(), night))) {
			return false;
		}
		return warm(level, water);
	}

	/** Whether a bank lies here on this night: a disc with a wandering edge, in one of the cells around. */
	public static boolean inBank(int x, int z, long night) {
		int cellX = Math.floorDiv(x, CELL);
		int cellZ = Math.floorDiv(z, CELL);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				long seed = mix(mix(mix(night) + cellX + dx) + cellZ + dz);
				if (fraction(seed, 0) >= BANK_CHANCE) {
					continue;
				}
				float centerX = (cellX + dx + fraction(seed, 1)) * CELL;
				float centerZ = (cellZ + dz + fraction(seed, 2)) * CELL;
				float radius = MIN_RADIUS + fraction(seed, 3) * (MAX_RADIUS - MIN_RADIUS);
				long edge = mix(seed + Math.floorDiv(x, EDGE_CELL) * 31L + Math.floorDiv(z, EDGE_CELL));
				radius *= 0.75F + 0.5F * fraction(edge, 0);
				float distanceX = x + 0.5F - centerX;
				float distanceZ = z + 0.5F - centerZ;
				if (distanceX * distanceX + distanceZ * distanceZ < radius * radius) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean warm(ClientLevel level, BlockPos water) {
		if (warm(level.getBiome(water))) {
			return true;
		}
		if (!level.getBiome(water).is(BiomeTags.IS_BEACH)) {
			return false;
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (warm(level.getBiome(water.relative(side, OFFSHORE)))) {
				return true;
			}
		}
		return false;
	}

	private static boolean warm(Holder<Biome> biome) {
		return biome.is(Biomes.WARM_OCEAN) || biome.is(Biomes.LUKEWARM_OCEAN) || biome.is(Biomes.DEEP_LUKEWARM_OCEAN);
	}

	static void tick(Minecraft minecraft, ClientLevel level) {
		age++;
		SPARKS.removeIf(spark -> age - spark.born >= spark.life);
		if (night(level) < DARK_ENOUGH) {
			return;
		}
		BlockPos around = minecraft.player.blockPosition();
		int tries = level.isRaining() ? RAIN_TRIES : CALM_TRIES;
		for (int i = 0; i < tries; i++) {
			BlockPos column = around.offset(RANDOM.nextIntBetweenInclusive(-REACH, REACH), 0, RANDOM.nextIntBetweenInclusive(-REACH, REACH));
			spark(level, column.getX() + RANDOM.nextFloat(), column.getZ() + RANDOM.nextFloat(), Mth.randomBetweenInclusive(RANDOM, 1, 2), Mth.randomBetweenInclusive(RANDOM, 6, 14));
		}
		// A swimmer at the surface, stirring the water around them as they go
		Vec3 moving = minecraft.player.getDeltaMovement();
		if (minecraft.player.isInWater() && moving.horizontalDistance() > 0.02) {
			stir(level, minecraft.player.position(), Mth.clamp((int) (moving.horizontalDistance() * 30), 1, 6), 1.2F);
		}
		// A boat's wake, behind it and along its sides
		for (AbstractBoat boat : level.getEntitiesOfClass(AbstractBoat.class, minecraft.player.getBoundingBox().inflate(BOAT_REACH))) {
			Vec3 speed = boat.getDeltaMovement();
			if (speed.horizontalDistance() > 0.03) {
				Vec3 behind = boat.position().subtract(speed.normalize().scale(1.2));
				stir(level, behind, Mth.clamp((int) (speed.horizontalDistance() * 25), 1, 10), 1.0F);
			}
		}
	}

	/** Water stirred here: a burst of sparks around, if it lies in a bank tonight. */
	public static void stir(ClientLevel level, Vec3 at, int count, float radius) {
		for (int i = 0; i < count; i++) {
			float x = (float) at.x + Mth.randomBetween(RANDOM, -radius, radius);
			float z = (float) at.z + Mth.randomBetween(RANDOM, -radius, radius);
			spark(level, x, z, Mth.randomBetweenInclusive(RANDOM, 1, 2), Mth.randomBetweenInclusive(RANDOM, 8, 20));
		}
	}

	private static void spark(ClientLevel level, float x, float z, int pixels, int life) {
		if (SPARKS.size() >= MAX_SPARKS) {
			return;
		}
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) - 1;
		BlockPos water = new BlockPos(Mth.floor(x), top, Mth.floor(z));
		if (!level.getFluidState(water).is(FluidTags.WATER) || !glows(level, water)) {
			return;
		}
		// Snapped to the block's pixels, as the foam is
		SPARKS.add(new Spark(Math.round(x * 16) * PIXEL, top + SURFACE, Math.round(z * 16) * PIXEL, pixels, age, life));
	}

	/** 0 by day, 1 at the dead of night. */
	private static float night(ClientLevel level) {
		return Mth.clamp(level.getSkyDarken() / DARKEST_SKY, 0, 1);
	}

	static void draw(LevelRenderContext context) {
		if (SPARKS.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		float time = age + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 camera = context.levelState().cameraRenderState.pos;
		context.submitNodeCollector().submitCustomGeometry(context.poseStack(), Foam.renderType(), (pose, buffer) -> {
			for (Spark spark : SPARKS) {
				draw(pose, buffer, spark, time, camera);
			}
		});
	}

	/** Brightest as it lights, dimming to the dark water's blue, and smaller by whole pixels: nothing in the game fades. */
	private static void draw(PoseStack.Pose pose, VertexConsumer buffer, Spark spark, float time, Vec3 camera) {
		float progress = Mth.clamp((time - spark.born) / spark.life, 0, 1);
		int pixels = Mth.ceil(spark.pixels * (1 - progress * progress));
		if (pixels <= 0) {
			return;
		}
		float size = pixels * PIXEL;
		int color = ARGB.srgbLerp(progress, GLOW, DIM);
		float x = (float) (spark.x - camera.x);
		float y = (float) (spark.y - camera.y);
		float z = (float) (spark.z - camera.z);
		buffer.addVertex(pose, x, y, z).setColor(color);
		buffer.addVertex(pose, x, y, z + size).setColor(color);
		buffer.addVertex(pose, x + size, y, z + size).setColor(color);
		buffer.addVertex(pose, x + size, y, z).setColor(color);
	}

	/** SplitMix64's finalizer: a well-mixed number from any. */
	private static long mix(long value) {
		long z = value + 0x9E3779B97F4A7C15L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/** A number from 0 to 1, from one of a seed's four 16-bit parts. */
	private static float fraction(long seed, int part) {
		return ((seed >>> (part * 16)) & 0xFFFF) / 65536F;
	}
}
