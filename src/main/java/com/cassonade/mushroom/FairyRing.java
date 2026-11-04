package com.cassonade.mushroom;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Fairy rings in mushroom fields: a circle of mushrooms, all red or all brown, a few blocks across, standing
 * on the mycelium with the middle left bare, as rings of fungi grow outwards from where they started. Only
 * vanilla mushrooms, placed as the world generates, so a world opened without the mod keeps them.
 */
public record FairyRing() implements Feature {
	public static final MapCodec<FairyRing> CODEC = MapCodec.unit(FairyRing::new);
	private static final Identifier ID = Identifier.fromNamespaceAndPath("cassonade", "fairy_ring");
	private static final int MIN_RADIUS = 3;
	private static final int MAX_RADIUS = 6;
	/** How far up or down a mushroom looks for the mycelium, around the ring's center. */
	private static final int SLOPE = 3;
	/** One spot in this many stays empty, for a ring grown rather than drawn. */
	private static final int GAPS = 6;

	public static void init() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, ID, CODEC);
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.MUSHROOM_FIELDS), GenerationStep.Decoration.VEGETAL_DECORATION,
				ResourceKey.create(Registries.PLACED_FEATURE, ID));
	}

	@Override
	public MapCodec<FairyRing> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		BlockState mushroom = (random.nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState();
		float radius = Mth.randomBetween(random, MIN_RADIUS, MAX_RADIUS);
		// One spot per block of the circle's length
		int spots = Mth.ceil(Mth.TWO_PI * radius);
		float start = random.nextFloat() * Mth.TWO_PI;
		int placed = 0;
		for (int i = 0; i < spots; i++) {
			if (random.nextInt(GAPS) == 0) {
				continue;
			}
			float angle = start + i * Mth.TWO_PI / spots;
			BlockPos column = origin.offset(Math.round(Mth.cos(angle) * radius), 0, Math.round(Mth.sin(angle) * radius));
			BlockPos spot = ground(level, column);
			if (spot != null && level.getBlockState(spot).isAir() && mushroom.canSurvive(level, spot)) {
				level.setBlock(spot, mushroom, Block.UPDATE_CLIENTS);
				placed++;
			}
		}
		return placed > 0;
	}

	/** The first free spot on the mycelium within a few blocks of the center's height, or null. */
	private static BlockPos ground(WorldGenLevel level, BlockPos column) {
		for (int dy = SLOPE; dy >= -SLOPE; dy--) {
			BlockPos spot = column.above(dy);
			if (level.getBlockState(spot.below()).is(Blocks.MYCELIUM) && level.getBlockState(spot).isAir()) {
				return spot;
			}
		}
		return null;
	}
}
