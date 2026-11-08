package com.cassonade.tweaks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;
import net.minecraft.world.level.storage.loot.LootTable;

import com.cassonade.Cassonade;
import com.cassonade.mixin.MineShaftCorridorAccessor;

/**
 * A miners' camp left in an abandoned mineshaft, one or two to a mineshaft: a straw bed against a corridor's wall, a cold
 * campfire and a barrel of their things against the other, the way between them left clear. Only in corridors without
 * rails or cobwebs, between two of their supports. The camps are chosen from the mineshaft's pieces and its start's
 * position alone, so every chunk agrees, and each is laid whole by the chunk it stands in.
 */
public final class MinersCamps {
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Cassonade.id("chests/miners_camp"));
	private static final int MAX_CAMPS = 2;
	/** A corridor's sections are 5 blocks long, a support at the third block of each. */
	private static final int SECTION = 5;

	private MinersCamps() {
	}

	/** A camp's blocks: the bed's head and foot along one wall, the campfire and the barrel along the other. */
	private record Camp(BlockPos head, BlockPos foot, BlockPos fire, BlockPos barrel, Direction along) {
		List<BlockPos> blocks() {
			return List.of(head, foot, fire, barrel);
		}

		boolean within(ChunkPos chunk) {
			return blocks().stream().allMatch(pos -> ChunkPos.containing(pos).equals(chunk));
		}
	}

	/** Lays the mineshaft's camps that stand in this chunk. */
	public static void place(WorldGenLevel level, PiecesContainer pieces, ChunkPos start, BoundingBox chunkBB) {
		ChunkPos here = new ChunkPos(chunkBB.minX() >> 4, chunkBB.minZ() >> 4);
		for (Camp camp : camps(pieces, start)) {
			if (camp.within(here) && fits(level, camp)) {
				lay(level, camp);
			}
		}
	}

	/** One or two camps, each in a different corridor, every one within a single chunk. */
	private static List<Camp> camps(PiecesContainer pieces, ChunkPos start) {
		RandomSource random = RandomSource.create(start.pack() * 0x5DEECE66DL + 11);
		List<Camp> candidates = new ArrayList<>();
		for (StructurePiece piece : pieces.pieces()) {
			if (!(piece instanceof MineshaftPieces.MineShaftCorridor corridor) || piece.getOrientation() == null) {
				continue;
			}
			MineShaftCorridorAccessor details = (MineShaftCorridorAccessor) corridor;
			if (details.cassonade$hasRails() || details.cassonade$spiderCorridor()) {
				continue;
			}
			// The last section ends at the corridor's end, without room past its support
			for (int section = 0; section < details.cassonade$numSections() - 1; section++) {
				Camp camp = camp(piece, section, random.nextBoolean());
				if (camp.within(ChunkPos.containing(camp.head))) {
					candidates.add(camp);
					break;
				}
			}
		}
		Util.shuffle(candidates, random);
		return candidates.subList(0, Math.min(1 + random.nextInt(MAX_CAMPS), candidates.size()));
	}

	/** Between the section's support and the next, its bed on the left wall or the right one. */
	private static Camp camp(StructurePiece piece, int section, boolean left) {
		int z = 3 + section * SECTION;
		int bed = left ? 0 : 2;
		int other = 2 - bed;
		return new Camp(world(piece, bed, z), world(piece, bed, z + 1), world(piece, other, z + 1), world(piece, other, z + 2), piece.getOrientation());
	}

	/** A block of the piece in the world, from its own coordinates: across the corridor, then along it, at its floor. */
	private static BlockPos world(StructurePiece piece, int x, int z) {
		BoundingBox box = piece.getBoundingBox();
		return switch (piece.getOrientation()) {
			case NORTH -> new BlockPos(box.minX() + x, box.minY(), box.maxZ() - z);
			case SOUTH -> new BlockPos(box.minX() + x, box.minY(), box.minZ() + z);
			case WEST -> new BlockPos(box.maxX() - z, box.minY(), box.minZ() + x);
			default -> new BlockPos(box.minX() + z, box.minY(), box.minZ() + x);
		};
	}

	/** Open where the camp goes, on a floor: a corridor built over a cave, or flooded, takes none. */
	private static boolean fits(WorldGenLevel level, Camp camp) {
		for (BlockPos pos : camp.blocks()) {
			if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).isSolidRender()) {
				return false;
			}
		}
		return true;
	}

	private static void lay(WorldGenLevel level, Camp camp) {
		// The head toward the corridor's start, so the foot faces the campfire's side of the camp
		Direction facing = camp.along.getOpposite();
		BlockState bed = Blocks.STRAW_BED.defaultBlockState().setValue(AbstractBedBlock.FACING, facing);
		level.setBlock(camp.head, bed.setValue(AbstractBedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
		level.setBlock(camp.foot, bed.setValue(AbstractBedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
		level.setBlock(camp.fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false).setValue(CampfireBlock.FACING, facing),
				Block.UPDATE_CLIENTS);
		level.setBlock(camp.barrel, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), Block.UPDATE_CLIENTS);
		RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(camp.barrel.asLong()), camp.barrel, LOOT);
	}
}
