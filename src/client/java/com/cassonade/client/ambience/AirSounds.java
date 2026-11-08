package com.cassonade.client.ambience;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import com.cassonade.Cassonade;

/**
 * Birds now and then outdoors, each from its own direction: by day, robins among trees in the warm lands, with cuckoos in
 * forests; owls in forests by night; frogs in the swamps from dusk on; crickets in the warm
 * lands by night and grasshoppers in savannas by day, a few at a time rather than an unbroken bed of sound; a lone wolf far off
 * in the taigas on full moon nights, now and then. Gulls and geese are only heard as a flock passes (Birds).
 */
public final class AirSounds {
	public static final Identifier MEADOW = Cassonade.id("meadow");
	public static final Identifier GULLS = Cassonade.id("gulls");
	public static final Identifier CRICKETS = Cassonade.id("crickets");
	public static final Identifier GRASSHOPPERS = Cassonade.id("grasshoppers");
	public static final Identifier FOREST_DAY = Cassonade.id("forest_day");
	public static final Identifier FOREST_NIGHT = Cassonade.id("forest_night");
	public static final Identifier WOLF = Cassonade.id("wolf");
	/** Heard only as a flock passes (Birds), from far off. */
	static final float GULL_VOLUME = 0.15F;

	private static final float MEADOW_VOLUME = 0.2F;
	/** Too hot for songbirds above this: deserts, savannas and badlands. */
	private static final float MEADOW_MAX_TEMPERATURE = 1.5F;
	private static final float FOREST_DAY_VOLUME = 0.4F;
	private static final float FOREST_NIGHT_VOLUME = 0.3F;
	private static final float BIRD_DISTANCE = 30;
	private static final float CALL_HEIGHT = 8;
	/** Each song from a bird of its own, some in the next tree, others far off. */
	private static final float SONG_NEAREST = 10;
	private static final float SONG_FARTHEST = 40;
	private static final float SONG_RANGE = 60;
	private static final float CRICKET_VOLUME = 0.05F;
	private static final float GRASSHOPPER_VOLUME = 0.06F;
	/** Colder than this, too cold for crickets to sing. */
	private static final float INSECT_MIN_TEMPERATURE = 0.5F;
	private static final int INSECT_NIGHT_FROM = 12500;
	private static final int INSECT_NIGHT_UNTIL = 23000;
	/** Between the start of two songs of about 8 seconds: crickets often overlap a little, a night being full of them. */
	private static final int MIN_CRICKET_WAIT = 60;
	private static final int MAX_CRICKET_WAIT = 240;
	private static final int MIN_GRASSHOPPER_WAIT = 200;
	private static final int MAX_GRASSHOPPER_WAIT = 800;
	private static final float FROG_VOLUME = 0.2F;
	private static final int FROGS_FROM = 11500;
	private static final int FROGS_UNTIL = 23500;
	private static final int MIN_FROG_WAIT = 30;
	private static final int MAX_FROG_WAIT = 200;
	/** A lone wolf howls once in a while, from far off, beyond where wolves are drawn: a moment of the night, not a chorus. */
	private static final float WOLF_VOLUME = 0.35F;
	private static final float WOLF_NEAREST = 70;
	private static final float WOLF_FARTHEST = 110;
	private static final float WOLF_RANGE = 160;
	private static final int WOLVES_FROM = 13000;
	private static final int WOLVES_UNTIL = 23000;
	private static final int MIN_WOLF_WAIT = 2400;
	private static final int MAX_WOLF_WAIT = 7200;
	private static final int MIN_BIRD_WAIT = 100;
	private static final int MAX_BIRD_WAIT = 400;
	/** How soon to look again where nothing calls. */
	private static final int RETRY = 40;
	private static final int TREE_REACH = 16;
	private static final int TREE_STEP = 4;
	private static final int COAST_REACH = 64;
	private static final int COAST_STEP = 16;
	private static final int COAST_DIRECTIONS = 8;
	private static final int WIND_FROM = 130;
	private static final int WIND_FULL = 200;
	private static final RandomSource RANDOM = RandomSource.create();
	private static int birdWait;
	private static int frogWait;
	private static int insectWait;
	private static int wolfWait;

	private AirSounds() {
	}

	static void tick(Minecraft minecraft, ClientLevel level) {
		BlockPos pos = minecraft.player.blockPosition();
		if (!Ambience.overworld(level)) {
			return;
		}
		// Under the leaves too, where the birds are, but not indoors
		if (level.getBrightness(LightLayer.SKY, pos) > 0) {
			if (--insectWait <= 0) {
				Identifier insects = insectsAt(level, pos);
				if (insects == null) {
					insectWait = RETRY;
				} else {
					insectWait = insects == CRICKETS ? Mth.randomBetweenInclusive(RANDOM, MIN_CRICKET_WAIT, MAX_CRICKET_WAIT)
							: Mth.randomBetweenInclusive(RANDOM, MIN_GRASSHOPPER_WAIT, MAX_GRASSHOPPER_WAIT);
					play(minecraft, insects);
				}
			}
			if (--frogWait <= 0) {
				if (frogsAt(level, pos)) {
					frogWait = Mth.randomBetweenInclusive(RANDOM, MIN_FROG_WAIT, MAX_FROG_WAIT);
					croak(minecraft);
				} else {
					frogWait = RETRY;
				}
			}
			if (--wolfWait <= 0) {
				if (wolvesAt(level, pos)) {
					wolfWait = Mth.randomBetweenInclusive(RANDOM, MIN_WOLF_WAIT, MAX_WOLF_WAIT);
					howl(minecraft);
				} else {
					wolfWait = RETRY;
				}
			}
			if (--birdWait <= 0) {
				Identifier sound = birdsAt(level, pos);
				if (sound == null) {
					birdWait = RETRY;
				} else {
					// An owl calls far less often than the day's birds sing
					birdWait = Mth.randomBetweenInclusive(RANDOM, MIN_BIRD_WAIT, MAX_BIRD_WAIT) * (sound == FOREST_NIGHT ? 3 : 1);
					float distance = Mth.randomBetween(RANDOM, SONG_NEAREST, SONG_FARTHEST);
					AmbientSound.of(sound, sound == MEADOW ? MEADOW_VOLUME : sound == FOREST_DAY ? FOREST_DAY_VOLUME : FOREST_NIGHT_VOLUME)
							.around(distance, CALL_HEIGHT).fading(SONG_RANGE).muffled().pitch(Mth.randomBetween(RANDOM, 0.95F, 1.05F)).play(minecraft);
				}
			}
		}
	}

	/** Crickets through the warm nights, savannas included, grasshoppers in savannas by day; none in the rain, by the sea or in the cold. */
	public static @Nullable Identifier insectsAt(ClientLevel level, BlockPos pos) {
		Holder<Biome> biome = level.getBiome(pos);
		if (level.isRaining() || coast(level, pos) || biome.value().coldEnoughToSnow(pos, level.getSeaLevel())) {
			return null;
		}
		long tick = Ambience.tickOfDay(level);
		if (Ambience.daytime(level) && biome.is(BiomeTags.IS_SAVANNA)) {
			return GRASSHOPPERS;
		}
		float temperature = biome.value().getBaseTemperature();
		// Savannas count among the very hot lands, but unlike deserts and badlands they are full of grass
		boolean mild = temperature >= INSECT_MIN_TEMPERATURE && temperature < MEADOW_MAX_TEMPERATURE;
		return tick >= INSECT_NIGHT_FROM && tick < INSECT_NIGHT_UNTIL && (mild || biome.is(BiomeTags.IS_SAVANNA)) ? CRICKETS : null;
	}

	static void play(Minecraft minecraft, Identifier insects) {
		playFrom(minecraft, insects, insects == CRICKETS ? CRICKET_VOLUME : GRASSHOPPER_VOLUME);
	}

	/** For tests: the next insects sing without waiting their turn. */
	public static void callInsects() {
		insectWait = 0;
	}

	static void croak(Minecraft minecraft) {
		playFrom(minecraft, SoundEvents.FROG_AMBIENT.location(), FROG_VOLUME);
	}

	/** The swamps' evening chorus: the game's own frogs, closer together than any of them calls. */
	public static boolean frogsAt(ClientLevel level, BlockPos pos) {
		long tick = Ambience.tickOfDay(level);
		Holder<Biome> biome = level.getBiome(pos);
		return tick >= FROGS_FROM && tick < FROGS_UNTIL && !level.isRaining() && (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP));
	}

	public static void howl(Minecraft minecraft) {
		AmbientSound.of(WOLF, WOLF_VOLUME).around(Mth.randomBetween(RANDOM, WOLF_NEAREST, WOLF_FARTHEST), CALL_HEIGHT).fading(WOLF_RANGE).muffled()
				.pitch(Mth.randomBetween(RANDOM, 0.95F, 1.05F)).play(minecraft);
	}

	/** A taiga's full moon night, without rain. */
	public static boolean wolvesAt(ClientLevel level, BlockPos pos) {
		long tick = Ambience.tickOfDay(level);
		return tick >= WOLVES_FROM && tick < WOLVES_UNTIL && !level.isRaining() && level.getBiome(pos).is(BiomeTags.IS_TAIGA)
				&& level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos) == MoonPhase.FULL_MOON;
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

	/** The sea or its shore: the stony shore too, which the game's beaches leave out. */
	static boolean coast(ClientLevel level, BlockPos pos) {
		Holder<Biome> biome = level.getBiome(pos);
		return biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_OCEAN) || biome.is(Biomes.STONY_SHORE);
	}

	/** Whether a beach or the sea lies around, close enough for gulls to come inland. */
	public static boolean nearCoast(ClientLevel level, BlockPos pos) {
		if (coast(level, pos)) {
			return true;
		}
		for (int reach = COAST_STEP; reach <= COAST_REACH; reach += COAST_STEP) {
			for (int i = 0; i < COAST_DIRECTIONS; i++) {
				float angle = i * Mth.TWO_PI / COAST_DIRECTIONS;
				if (coast(level, pos.offset(Mth.floor(Mth.cos(angle) * reach), 0, Mth.floor(Mth.sin(angle) * reach)))) {
					return true;
				}
			}
		}
		return false;
	}

	/** 0 below the height where the wind starts, 1 from where it is full. */
	static float windStrength(ClientLevel level, int y) {
		return Mth.clamp((y - WIND_FROM) / (float) (WIND_FULL - WIND_FROM), 0, 1);
	}

	/** From a direction of its own, at a volume of its own. */
	private static void playFrom(Minecraft minecraft, Identifier sound, float volume) {
		AmbientSound.of(sound, volume).around(BIRD_DISTANCE, CALL_HEIGHT).pitch(Mth.randomBetween(RANDOM, 0.95F, 1.05F)).play(minecraft);
	}
}
