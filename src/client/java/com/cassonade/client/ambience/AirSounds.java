package com.cassonade.client.ambience;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;

/**
 * Birds now and then outdoors, each from its own direction: by day, robins among trees in the warm lands, with cuckoos in
 * forests; owls in forests by night. Gulls and geese are only heard as a flock passes (Birds).
 */
public final class AirSounds {
	public static final Identifier MEADOW = Cassonade.id("meadow");
	public static final Identifier GULLS = Cassonade.id("gulls");
	public static final Identifier FOREST_DAY = Cassonade.id("forest_day");
	public static final Identifier FOREST_NIGHT = Cassonade.id("forest_night");
	/** Heard only as a flock passes (Birds), from far off. */
	static final float GULL_VOLUME = 0.15F;

	private static final float MEADOW_VOLUME = 0.08F;
	/** Too hot for songbirds above this: deserts, savannas and badlands. */
	private static final float MEADOW_MAX_TEMPERATURE = 1.5F;
	private static final float FOREST_DAY_VOLUME = 0.16F;
	private static final float FOREST_NIGHT_VOLUME = 0.12F;
	private static final float BIRD_DISTANCE = 30;
	private static final int MIN_BIRD_WAIT = 100;
	private static final int MAX_BIRD_WAIT = 400;
	/** How soon to look again where nothing calls. */
	private static final int RETRY = 40;
	private static final int TREE_REACH = 16;
	private static final int TREE_STEP = 4;
	private static final int WIND_FROM = 130;
	private static final int WIND_FULL = 200;
	private static final RandomSource RANDOM = RandomSource.create();
	private static int birdWait;

	private AirSounds() {
	}

	static void tick(Minecraft minecraft, ClientLevel level) {
		BlockPos pos = minecraft.player.blockPosition();
		if (!Ambience.overworld(level)) {
			return;
		}
		// Under the leaves too, where the birds are, but not indoors
		if (level.getBrightness(LightLayer.SKY, pos) > 0) {
			if (--birdWait <= 0) {
				Identifier sound = birdsAt(level, pos);
				if (sound == null) {
					birdWait = RETRY;
				} else {
					// An owl calls far less often than the day's birds sing
					birdWait = Mth.randomBetweenInclusive(RANDOM, MIN_BIRD_WAIT, MAX_BIRD_WAIT) * (sound == FOREST_NIGHT ? 3 : 1);
					playFrom(minecraft, sound, sound == MEADOW ? MEADOW_VOLUME : sound == FOREST_DAY ? FOREST_DAY_VOLUME : FOREST_NIGHT_VOLUME);
				}
			}
		}
	}

	/** What calls here at this hour, if anything. */
	public static @Nullable Identifier birdsAt(ClientLevel level, BlockPos pos) {
		if (level.isRaining()) {
			return null;
		}
		Holder<Biome> biome = level.getBiome(pos);
		if (biome.is(BiomeTags.IS_JUNGLE)) {
			return null;
		}
		if (biome.is(BiomeTags.IS_FOREST) && !Ambience.daytime(level)) {
			return FOREST_NIGHT;
		}
		if (!Ambience.daytime(level) || !nearTrees(level, pos)) {
			return null;
		}
		if (biome.is(BiomeTags.IS_FOREST)) {
			return FOREST_DAY;
		}
		return !coast(level, pos) && !biome.value().coldEnoughToSnow(pos, level.getSeaLevel()) && biome.value().getBaseTemperature() < MEADOW_MAX_TEMPERATURE ? MEADOW : null;
	}

	/** Whether a tree stands near: the top of some column around is leaves or a trunk. */
	private static boolean nearTrees(ClientLevel level, BlockPos pos) {
		for (int x = -TREE_REACH; x <= TREE_REACH; x += TREE_STEP) {
			for (int z = -TREE_REACH; z <= TREE_REACH; z += TREE_STEP) {
				int columnX = pos.getX() + x;
				int columnZ = pos.getZ() + z;
				BlockState top = level.getBlockState(new BlockPos(columnX, level.getHeight(Heightmap.Types.MOTION_BLOCKING, columnX, columnZ) - 1, columnZ));
				if (top.is(BlockTags.LEAVES) || top.is(BlockTags.LOGS)) {
					return true;
				}
			}
		}
		return false;
	}

	static boolean coast(ClientLevel level, BlockPos pos) {
		Holder<Biome> biome = level.getBiome(pos);
		return biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_OCEAN);
	}

	/** 0 below the height where the wind starts, 1 from where it is full. */
	static float windStrength(ClientLevel level, int y) {
		return Mth.clamp((y - WIND_FROM) / (float) (WIND_FULL - WIND_FROM), 0, 1);
	}

	/** From a direction of its own, at a volume of its own: the game's attenuation would silence it at this distance. */
	private static void playFrom(Minecraft minecraft, Identifier sound, float volume) {
		float angle = RANDOM.nextFloat() * Mth.TWO_PI;
		Vec3 from = minecraft.player.getEyePosition().add(Mth.cos(angle) * BIRD_DISTANCE, 8, Mth.sin(angle) * BIRD_DISTANCE);
		minecraft.getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.AMBIENT, volume, Mth.randomBetween(RANDOM, 0.95F, 1.05F),
				SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, from.x, from.y, from.z, false));
	}
}
