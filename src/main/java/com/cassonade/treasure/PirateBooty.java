package com.cassonade.treasure;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootTable;

import com.cassonade.Cassonade;

/**
 * A pirate's booty in lush caves: a very rare hoard of barrels piled on the cave floor, some stood up, some on
 * their side, one on top of another, each holding gold, gems and a few rare things. Only vanilla barrels with a
 * loot table, placed as the world generates, so a world opened without the mod keeps them.
 */
public record PirateBooty() implements Feature {
	public static final MapCodec<PirateBooty> CODEC = MapCodec.unit(PirateBooty::new);
	private static final Identifier ID = Cassonade.id("pirate_booty");
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Cassonade.id("chests/pirate_booty"));
	private static final int MIN_BARRELS = 3;
	private static final int MAX_BARRELS = 6;
	/** Free spots of floor needed around the center: a cave rather than a crack. */
	private static final int ROOMY = 8;
	/** Open blocks needed over the center. */
	private static final int HEADROOM = 4;
	/** How far up or down the hoard looks for the cave's floor from where it was placed. */
	private static final int REACH = 16;
	private static final ResourceKey<PlacedFeature> PLACED = ResourceKey.create(Registries.PLACED_FEATURE, ID);

	public static void init() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, ID, CODEC);
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.LUSH_CAVES), GenerationStep.Decoration.VEGETAL_DECORATION, PLACED);
	}

	@Override
	public MapCodec<PirateBooty> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		BlockPos center = floor(level, origin);
		if (center == null) {
			return false;
		}
		// The free spots of the floor around, the center first, so the pile stays together
		List<BlockPos> spots = new ArrayList<>();
		spots.add(center);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if ((dx != 0 || dz != 0) && dx * dx + dz * dz <= 5) {
					for (int dy = 1; dy >= -1; dy--) {
						BlockPos spot = center.offset(dx, dy, dz);
						if (standsOn(level, spot)) {
							spots.add(spot);
							break;
						}
					}
				}
			}
		}
		// Out in a cave of the lush caves themselves, with room around and overhead, not in a pocket of the rock
		if (spots.size() < ROOMY || !level.getBiome(center).is(Biomes.LUSH_CAVES)) {
			return false;
		}
		for (int dy = 1; dy <= HEADROOM; dy++) {
			if (!open(level.getBlockState(center.above(dy)))) {
				return false;
			}
		}
		List<BlockPos> rest = new ArrayList<>(spots.subList(1, spots.size()));
		Util.shuffle(rest, random);
		int barrels = Math.min(MIN_BARRELS + random.nextInt(MAX_BARRELS - MIN_BARRELS + 1), spots.size());
		List<BlockPos> placed = new ArrayList<>();
		placed.add(center);
		placed.addAll(rest.subList(0, barrels - 1));
		for (BlockPos pos : placed) {
			barrel(level, random, pos);
		}
		// One more on top of the pile, when there is room for it
		BlockPos top = center.above();
		if (open(level.getBlockState(top)) && open(level.getBlockState(top.above()))) {
			barrel(level, random, top);
		}
		return true;
	}

	private static void barrel(WorldGenLevel level, RandomSource random, BlockPos pos) {
		// Stood up or lying on its side, as a pile left in a hurry
		Direction facing = random.nextInt(3) == 0 ? Direction.Plane.HORIZONTAL.getRandomDirection(random) : Direction.UP;
		level.setBlock(pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing), Block.UPDATE_CLIENTS);
		// The top half of a tall plant the barrel took the place of would float
		BlockState above = level.getBlockState(pos.above());
		if (above.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		RandomizableContainer.setBlockEntityLootTable(level, random, pos, LOOT);
	}

	/**
	 * The cave's floor nearest to where the hoard was placed, with room above, in its column or a few blocks aside
	 * (staying in its chunk's decoration area): or null in solid rock.
	 */
	private static BlockPos floor(WorldGenLevel level, BlockPos origin) {
		for (int ring = 0; ring <= 2; ring++) {
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dz = -ring; dz <= ring; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
						continue;
					}
					BlockPos column = origin.offset(dx * 4, 0, dz * 4);
					for (int dy = 0; dy <= REACH; dy++) {
						for (int sign : new int[] {-1, 1}) {
							BlockPos spot = column.above(sign * dy);
							if (standsOn(level, spot) && open(level.getBlockState(spot.above()))) {
								return spot;
							}
						}
					}
				}
			}
		}
		return null;
	}

	/** Air, or the moss's carpet and plants that cover a lush cave's floor, which a barrel takes the place of. */
	private static boolean open(BlockState state) {
		return state.canBeReplaced() && state.getFluidState().isEmpty();
	}

	/** Open over solid ground, not over water: a barrel set there stays. */
	private static boolean standsOn(WorldGenLevel level, BlockPos spot) {
		if (spot.getY() <= level.getMinY() || !open(level.getBlockState(spot))) {
			return false;
		}
		BlockState below = level.getBlockState(spot.below());
		return below.isFaceSturdy(level, spot.below(), Direction.UP) && below.getFluidState().isEmpty();
	}
}
