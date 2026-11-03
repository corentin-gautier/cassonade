package com.cassonade.client.ambience;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Specks in the air and the water: what floats in the sea around a swimmer, dust falling from cave ceilings, pollen around grass and trees by day, in savannas with dry dust, and on high ground what the wind
 * carries past. The game's smallest particle, added directly, so no new particle type has to exist on the server.
 */
public final class Motes {
	private static final Identifier SPRITE = Identifier.withDefaultNamespace("generic_0");
	private static final float RANGE = 12;
	/** Places tried a tick, in a cave where few are right under the rock, and outdoors where only some are near greenery. */
	private static final int CAVE_TRIES = 32;
	private static final int POLLEN_TRIES = 16;
	private static final int GREENERY_REACH = 2;
	private static final int GROUND_REACH = 5;
	private static final int CAVE_DEPTH = 8;
	/** Blocks a tick: slow enough to read as dust, fast enough to see it is falling. */
	private static final float DUST_FALL = 0.025F;
	private static final float MIN_POLLEN_TEMPERATURE = 0.3F;
	private static final float MAX_POLLEN_TEMPERATURE = 1.2F;
	private static final int WATER_TRIES = 8;
	private static final float[] SILT = {0.78F, 0.86F, 0.84F};
	private static final float[] DUST = {0.92F, 0.88F, 0.78F};
	private static final float[] POLLEN = {1, 0.93F, 0.55F};
	private static final float[] DRY_DUST = {0.9F, 0.72F, 0.46F};
	private static final float[] CARRIED = {0.95F, 0.95F, 0.95F};
	private static final RandomSource RANDOM = RandomSource.create();

	private Motes() {
	}

	/**
	 * The swell's pull on whatever floats in the water at a place: neighbors move together, back and forth over a few seconds,
	 * along a slower current. Shared with vanilla's own underwater particles (SuspendedParticleMixin).
	 */
	public static Vec3 swell(ClientLevel level, double x, double y, double z) {
		float time = level.getGameTime();
		return new Vec3(0.02F * Mth.sin(time * 0.05F + (float) (y * 0.3 + z * 0.15)) + 0.012F * Mth.sin(time * 0.013F + (float) (z * 0.08)),
				0.01F * Mth.sin(time * 0.07F + (float) (x * 0.2 + z * 0.2)),
				0.02F * Mth.sin(time * 0.04F + (float) (y * 0.3 + x * 0.15)) + 0.012F * Mth.sin(time * 0.017F + (float) (x * 0.08)));
	}

	static void tick(Minecraft minecraft, ClientLevel level) {
		if (!Ambience.overworld(level) || minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
			return;
		}
		Vec3 eye = minecraft.player.getEyePosition();
		TextureAtlasSprite sprite = minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(SPRITE);
		if (level.getFluidState(BlockPos.containing(eye)).is(FluidTags.WATER)) {
			for (int i = 0; i < WATER_TRIES; i++) {
				BlockPos pos = BlockPos.containing(eye.add(offset(), offset(), offset()));
				if (level.getFluidState(pos).is(FluidTags.WATER)) {
					minecraft.particleEngine.add(new Mote(level, pos, sprite, SILT, 0.006F, 0, 0));
				}
			}
			return;
		}
		for (int i = 0; i < CAVE_TRIES; i++) {
			BlockPos pos = BlockPos.containing(eye.add(offset(), offset(), offset()));
			if (!level.getBlockState(pos).isAir() || !level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
				continue;
			}
			if (inCave(level, pos)) {
				if (underCeiling(level, pos)) {
					minecraft.particleEngine.add(new Mote(level, pos, sprite, DUST, 0.006F, 0, DUST_FALL));
				}
			} else if (level.getBrightness(LightLayer.SKY, pos) == 0) {
				// A house's rooms hold nothing
				continue;
			} else if (AirSounds.windStrength(level, pos.getY()) > 0) {
				if (i == 0 && !level.isRaining()) {
					minecraft.particleEngine.add(new Mote(level, pos, sprite, CARRIED, 0.01F, 0.22F, 0));
				}
			} else if (i >= POLLEN_TRIES || !level.isBrightOutside() || level.isRaining()) {
				continue;
			} else if (level.getBiome(pos).is(BiomeTags.IS_SAVANNA)) {
				// Too hot to be temperate, and the air full of it: pollen over the grass, dust raised from any dry ground
				if (aboveGreenery(level, pos)) {
					minecraft.particleEngine.add(new Mote(level, pos, sprite, i % 2 == 0 ? POLLEN : DRY_DUST, 0.006F, 0, 0));
				} else if (aboveGround(level, pos)) {
					minecraft.particleEngine.add(new Mote(level, pos, sprite, DRY_DUST, 0.006F, 0, 0));
				}
			} else if (temperate(level, pos) && aboveGreenery(level, pos)) {
				minecraft.particleEngine.add(new Mote(level, pos, sprite, POLLEN, 0.006F, 0, 0));
			}
		}
	}

	private static float offset() {
		return (RANDOM.nextFloat() * 2 - 1) * RANGE;
	}

	/** No light from the sky and well under the surface: a house's rooms have their roof close above. */
	public static boolean inCave(ClientLevel level, BlockPos pos) {
		return level.getBrightness(LightLayer.SKY, pos) == 0
				&& level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - pos.getY() >= CAVE_DEPTH;
	}

	/** Rock or anything else solid right above: water does not shed dust. */
	public static boolean underCeiling(ClientLevel level, BlockPos pos) {
		BlockState above = level.getBlockState(pos.above());
		return !above.isAir() && above.getFluidState().isEmpty();
	}

	/** The first thing just under this place is grass or part of a tree: none over rock, sand or water, nor high in the air. */
	private static boolean aboveGreenery(ClientLevel level, BlockPos pos) {
		BlockPos.MutableBlockPos below = pos.mutable();
		for (int i = 0; i < GREENERY_REACH; i++) {
			BlockState state = level.getBlockState(below.move(Direction.DOWN));
			if (!state.isAir()) {
				return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SHORT_GRASS)
						|| state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN);
			}
		}
		return false;
	}

	/** Some solid block not far under this place: none over water, nor high in the air. */
	private static boolean aboveGround(ClientLevel level, BlockPos pos) {
		BlockPos.MutableBlockPos below = pos.mutable();
		for (int i = 0; i < GROUND_REACH; i++) {
			BlockState state = level.getBlockState(below.move(Direction.DOWN));
			if (!state.isAir()) {
				return state.getFluidState().isEmpty();
			}
		}
		return false;
	}

	private static boolean temperate(ClientLevel level, BlockPos pos) {
		float temperature = level.getBiome(pos).value().getBaseTemperature();
		return temperature >= MIN_POLLEN_TEMPERATURE && temperature <= MAX_POLLEN_TEMPERATURE;
	}

	private static final class Mote extends SingleQuadParticle {
		private static final float FADE_TIME = 0.25F;
		private static final float ALPHA = 0.85F;
		private static final float POLLEN_ALPHA = 0.55F;
		private final float drift;
		private final float windX;
		private final float fall;
		private final float alpha;
		private final boolean inWater;

		Mote(ClientLevel level, BlockPos pos, TextureAtlasSprite sprite, float[] color, float drift, float wind, float fall) {
			super(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sprite);
			this.drift = drift;
			windX = wind;
			this.fall = fall;
			alpha = color == POLLEN || color == SILT ? POLLEN_ALPHA : ALPHA;
			inWater = color == SILT;
			hasPhysics = false;
			gravity = 0;
			friction = 1;
			quadSize = 0.06F;
			lifetime = wind > 0 ? 60 : Mth.randomBetweenInclusive(random, 120, 240);
			xd = wind + (random.nextFloat() - 0.5F) * drift;
			yd = (random.nextFloat() - 0.5F) * drift - fall;
			zd = (random.nextFloat() - 0.5F) * drift + wind / 3;
			if (inWater) {
				setPos(pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble());
			} else if (fall > 0) {
				// Anywhere along the ceiling, not from the middle of each block
				setPos(pos.getX() + random.nextDouble(), pos.getY() + 0.9, pos.getZ() + random.nextDouble());
			}
			setColor(color[0], color[1], color[2]);
			setAlpha(0);
		}

		@Override
		public SingleQuadParticle.Layer getLayer() {
			return SingleQuadParticle.Layer.TRANSLUCENT;
		}

		@Override
		public void tick() {
			super.tick();
			float progress = (float) age / lifetime;
			setAlpha(alpha * Mth.clamp(Math.min(progress, 1 - progress) / FADE_TIME, 0, 1));
			// Wanders on the air or the water, and is gone if it wanders out of it
			xd = windX + Mth.clamp(xd - windX + (random.nextFloat() - 0.5F) * drift * 0.3F, -drift, drift);
			yd = -fall + Mth.clamp(yd + fall + (random.nextFloat() - 0.5F) * drift * 0.3F, -drift, drift);
			if (inWater) {
				Vec3 swell = swell(level, x, y, z);
				xd += swell.x;
				yd += swell.y;
				zd += swell.z;
			}
			BlockPos at = BlockPos.containing(x, y, z);
			if (inWater ? !level.getFluidState(at).is(FluidTags.WATER) : !level.getBlockState(at).isAir()) {
				remove();
			}
		}
	}
}
