package com.cassonade.treasure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import com.cassonade.Cassonade;

/**
 * A pirate's booty in lush caves: a very rare hoard of barrels piled on the cave floor, some stood up, some on
 * their side, one on top of another, each holding gold, gems and a few rare things. A structure rather than a
 * decoration, so a shipwreck's pirate map and `/locate` can find it: a chunk over the lush caves is chosen before it
 * exists, then the barrels go on the cave floor found in it, as the noise alone cannot tell where the floor is. Only vanilla barrels with a loot table, so a world opened
 * without the mod keeps them.
 */
public final class PirateBooty extends Structure {
	public static final MapCodec<PirateBooty> CODEC = simpleCodec(PirateBooty::new);
	private static final Identifier ID = Cassonade.id("pirate_booty");
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Cassonade.id("chests/pirate_booty"));
	/** What a pirate map leads to. */
	public static final TagKey<Structure> MAP_TAG = TagKey.create(Registries.STRUCTURE, Cassonade.id("on_pirate_maps"));
	private static final StructureType<PirateBooty> TYPE = () -> CODEC;
	private static final StructurePieceType.ContextlessType PIECE = Piece::new;
	private static final int MIN_BARRELS = 3;
	private static final int MAX_BARRELS = 6;
	/** Free spots of floor needed around the center: a cave rather than a crack. */
	private static final int ROOMY = 8;
	/** Open blocks needed over the center. */
	private static final int HEADROOM = 4;
	/** The heights the hoard lies between: deep enough to be a cave, as the lush caves are. */
	private static final int MIN_ABOVE_BOTTOM = 8;
	private static final int MAX_Y = 48;
	/** The columns read in the noise, on each axis. */
	private static final int[] COLUMNS = {4, 8, 12};
	/** The pile's center keeps this far from the chunk's edges, so all of it stays in the chunk. */
	private static final int MARGIN = 2;

	public PirateBooty(StructureSettings settings) {
		super(settings);
	}

	public static void init() {
		Registry.register(BuiltInRegistries.STRUCTURE_TYPE, ID, TYPE);
		Registry.register(BuiltInRegistries.STRUCTURE_PIECE, ID, PIECE);
	}

	@Override
	public StructureType<?> type() {
		return TYPE;
	}

	/**
	 * A chunk with a cave in the lush caves, from the terrain's noise, all a structure can read before the chunk
	 * exists. It fills the caves below sea level with water and misses the carvers, so it only tells there is a cave
	 * at about that height: the piece then finds the real floor. Without it, most chunks of the lush caves biome are
	 * solid rock, and a map would lead to nothing.
	 */
	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int minY = context.heightAccessor().getMinY() + MIN_ABOVE_BOTTOM;
		// The biome first, which costs little, so most chunks stop here
		if (!context.couldStructureExistInColumn(context.chunkPos().getBlockX(8), context.chunkPos().getBlockZ(8), minY, MAX_Y)) {
			return Optional.empty();
		}
		List<BlockPos> floors = new ArrayList<>();
		for (int cx : COLUMNS) {
			for (int cz : COLUMNS) {
				int x = context.chunkPos().getBlockX(cx);
				int z = context.chunkPos().getBlockZ(cz);
				NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
				for (int y = minY; y <= MAX_Y; y++) {
					if (cave(column.getBlock(y)) && !cave(column.getBlock(y - 1)) && headroom(column, y)
							&& context.validBiome().test(context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)))) {
						floors.add(new BlockPos(x, y, z));
					}
				}
			}
		}
		if (floors.isEmpty()) {
			return Optional.empty();
		}
		BlockPos center = floors.get(context.random().nextInt(floors.size()));
		ChunkPos chunk = context.chunkPos();
		return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new Piece(chunk, minY, center))));
	}

	/** Open in the noise: water counts, as a column read alone fills every cave below sea level with it. */
	private static boolean cave(BlockState state) {
		return state.isAir() || !state.getFluidState().isEmpty();
	}

	private static boolean headroom(NoiseColumn column, int y) {
		for (int dy = 1; dy <= HEADROOM; dy++) {
			if (!cave(column.getBlock(y + dy))) {
				return false;
			}
		}
		return true;
	}

	/** The whole chunk between the hoard's heights: the floor is looked for all over it. */
	public static final class Piece extends StructurePiece {
		Piece(ChunkPos chunk, int minY, BlockPos center) {
			super(PIECE, 0, new BoundingBox(chunk.getMinBlockX(), minY - 1, chunk.getMinBlockZ(), chunk.getMaxBlockX(), MAX_Y + HEADROOM + 1, chunk.getMaxBlockZ()));
		}

		Piece(CompoundTag tag) {
			super(PIECE, tag);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB,
				ChunkPos chunkPos, BlockPos referencePos) {
			List<BlockPos> spots = floor(level, this.boundingBox);
			if (spots == null) {
				return;
			}
			BlockPos center = spots.getFirst();
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
		}
	}

	/**
	 * The cave floor nearest to the chunk's center in the lush caves, with room overhead and floor around: its spots,
	 * the center first so the pile stays together. Without such room, the roomiest floor found, or null when the
	 * chunk has none, its caves flooded or carved away.
	 */
	private static List<BlockPos> floor(WorldGenLevel level, BoundingBox box) {
		BlockPos middle = box.getCenter();
		List<BlockPos> candidates = new ArrayList<>();
		for (BlockPos spot : BlockPos.betweenClosed(box.minX() + MARGIN, box.minY() + 1, box.minZ() + MARGIN, box.maxX() - MARGIN, MAX_Y, box.maxZ() - MARGIN)) {
			if (standsOn(level, spot) && headroom(level, spot)) {
				candidates.add(spot.immutable());
			}
		}
		candidates.sort((a, b) -> Integer.compare(a.distManhattan(middle), b.distManhattan(middle)));
		List<BlockPos> best = null;
		for (BlockPos center : candidates) {
			if (!level.getBiome(center).is(Biomes.LUSH_CAVES)) {
				continue;
			}
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
			// Out in a cave, with room around, not in a pocket of the rock
			if (spots.size() >= ROOMY) {
				return spots;
			}
			best = best == null || spots.size() > best.size() ? spots : best;
		}
		// A smaller pile rather than none, as a map may lead here
		return best != null && best.size() >= MIN_BARRELS ? best : null;
	}

	private static boolean headroom(WorldGenLevel level, BlockPos spot) {
		for (int dy = 1; dy <= HEADROOM; dy++) {
			if (!open(level.getBlockState(spot.above(dy)))) {
				return false;
			}
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
