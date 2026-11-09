package com.cassonade.dwelling;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import com.cassonade.Cassonade;

/**
 * The fisherman's shack, built on a river's bank: its back on the ground, about at the water's level, and the end of
 * its deck, where the ladder goes down, over water deep enough for a boat. A jigsaw piece would stand in the middle of
 * the chunk, turned at random, as often in the middle of the river as on dry land: this one looks over its chunk, from
 * the terrain's heights alone, for a spot and a turn where the shack fits the bank, and is not built without one.
 */
public final class FishermansShack extends Structure {
	public static final MapCodec<FishermansShack> CODEC = simpleCodec(FishermansShack::new);
	private static final Identifier ID = Cassonade.id("fishermans_shack");
	private static final Identifier TEMPLATE = Cassonade.id("dwelling/fishermans_shack");
	private static final StructureType<FishermansShack> TYPE = () -> CODEC;
	private static final StructurePieceType.StructureTemplateType PIECE = Piece::new;
	/** The deck's layer in the template: it stands a block over the water. */
	private static final int DECK = 4;
	/** In the template: behind the shack, on the ground. */
	private static final int[][] BACK = {{0, 0}, {3, 0}, {6, 0}, {3, 2}};
	/** In the template: the deck's end and the ladder, over the water. */
	private static final int[][] FRONT = {{0, 9}, {6, 9}, {3, 10}};
	/** How deep the water under the deck's end is at least. */
	private static final int DEPTH = 2;
	/** How far above the water the bank under the shack's back may rise. */
	private static final int BANK = 1;
	/** Spots looked at in the chunk, every this many blocks on each axis. */
	private static final int STEP = 2;

	public FishermansShack(StructureSettings settings) {
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

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int sea = context.chunkGenerator().getSeaLevel();
		// The biome first, which costs little, so most chunks stop here
		if (!river(context, 0, sea, 0) && !river(context, 15, sea, 0) && !river(context, 0, sea, 15) && !river(context, 15, sea, 15) && !river(context, 8, sea, 8)) {
			return Optional.empty();
		}
		Heights heights = new Heights(context);
		List<Rotation> rotations = new ArrayList<>(List.of(Rotation.values()));
		Util.shuffle(rotations, context.random());
		List<BlockPos> origins = new ArrayList<>();
		for (int dx = 0; dx < 16; dx += STEP) {
			for (int dz = 0; dz < 16; dz += STEP) {
				origins.add(new BlockPos(context.chunkPos().getBlockX(dx), sea - DECK, context.chunkPos().getBlockZ(dz)));
			}
		}
		Util.shuffle(origins, context.random());
		for (BlockPos origin : origins) {
			for (Rotation rotation : rotations) {
				if (fits(heights, sea, origin, rotation)) {
					// Its biome read under the deck's end, in the river
					BlockPos water = at(origin, rotation, FRONT[2]).atY(sea - 1);
					return Optional.of(new GenerationStub(water, builder -> builder.addPiece(new Piece(context.structureTemplateManager(), origin, rotation))));
				}
			}
		}
		return Optional.empty();
	}

	private static boolean river(GenerationContext context, int dx, int y, int dz) {
		return context.validBiome().test(context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(context.chunkPos().getBlockX(dx)), QuartPos.fromBlock(y),
				QuartPos.fromBlock(context.chunkPos().getBlockZ(dz))));
	}

	/** Whether, there and so turned, the shack's back is on the bank and its deck's end over deep enough water. */
	private static boolean fits(Heights heights, int sea, BlockPos origin, Rotation rotation) {
		for (int[] point : FRONT) {
			BlockPos pos = at(origin, rotation, point);
			// The first free block is the sea's level over water, its floor a few blocks lower
			if (heights.surface(pos) != sea || heights.floor(pos) > sea - DEPTH) {
				return false;
			}
		}
		for (int[] point : BACK) {
			BlockPos pos = at(origin, rotation, point);
			int ground = heights.floor(pos);
			if (heights.surface(pos) != ground || ground < sea || ground > sea + BANK) {
				return false;
			}
		}
		return true;
	}

	private static BlockPos at(BlockPos origin, Rotation rotation, int[] point) {
		return origin.offset(StructureTemplate.transform(new BlockPos(point[0], 0, point[1]), Mirror.NONE, rotation, BlockPos.ZERO));
	}

	/** The terrain's heights, each column read once: the same ones come back for every spot and turn tried. */
	private static final class Heights {
		private final GenerationContext context;
		private final Long2IntMap surface = new Long2IntOpenHashMap();
		private final Long2IntMap floor = new Long2IntOpenHashMap();

		Heights(GenerationContext context) {
			this.context = context;
		}

		/** The first free block over the column, water counting as taken. */
		int surface(BlockPos pos) {
			return this.surface.computeIfAbsent(BlockPos.asLong(pos.getX(), 0, pos.getZ()), _ -> height(pos, Heightmap.Types.WORLD_SURFACE_WG));
		}

		/** The first block over the ground, water counting as free. */
		int floor(BlockPos pos) {
			return this.floor.computeIfAbsent(BlockPos.asLong(pos.getX(), 0, pos.getZ()), _ -> height(pos, Heightmap.Types.OCEAN_FLOOR_WG));
		}

		private int height(BlockPos pos, Heightmap.Types type) {
			return this.context.chunkGenerator().getBaseHeight(pos.getX(), pos.getZ(), type, this.context.heightAccessor(), this.context.randomState());
		}
	}

	/** The shack's template, turned to face the water; its jigsaw block, kept for /dev edit, is replaced as a jigsaw piece's is. */
	public static final class Piece extends TemplateStructurePiece {
		Piece(StructureTemplateManager templates, BlockPos origin, Rotation rotation) {
			super(PIECE, 0, templates, TEMPLATE, TEMPLATE.toString(), settings(rotation), origin);
		}

		Piece(StructureTemplateManager templates, CompoundTag tag) {
			super(PIECE, tag, templates, _ -> settings(tag.read("Rot", Rotation.CODEC).orElse(Rotation.NONE)));
		}

		private static StructurePlaceSettings settings(Rotation rotation) {
			return new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE)
					.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).addProcessor(JigsawReplacementProcessor.INSTANCE)
					.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			super.addAdditionalSaveData(context, tag);
			tag.store("Rot", Rotation.CODEC, this.placeSettings.getRotation());
		}

		@Override
		protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB) {
		}
	}
}
