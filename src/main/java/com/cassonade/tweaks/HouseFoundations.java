package com.cassonade.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * Village houses set on a slope, a street's height taken from the ground before the village's terrain was shaped,
 * may overhang a hollow, as in vanilla; the towns, larger, show it more. Once the houses stand, each one's bottom
 * layer is carried down to the ground in its own material, as vanilla pours a foundation under mansions.
 */
public final class HouseFoundations {
	/** Deeper than this, the hollow is left as it is rather than raising a tower under the house. */
	private static final int MAX_DEPTH = 8;

	private HouseFoundations() {
	}

	/** Whether the piece is one of the village's houses, taverns and farms included. */
	public static boolean is(StructurePiece piece) {
		return piece instanceof PoolElementStructurePiece house && house.getElement().toString().contains("/houses/");
	}

	/** Carries the house's bottom layer down to the ground, in the part of the house within the area. */
	public static void pour(WorldGenLevel level, BoundingBox house, BoundingBox area) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = area.minX(); x <= area.maxX(); x++) {
			for (int z = area.minZ(); z <= area.maxZ(); z++) {
				pos.set(x, house.minY(), z);
				BlockState floor = level.getBlockState(pos);
				if (!floor.isCollisionShapeFullBlock(level, pos)) {
					continue;
				}
				// Grass and paths need daylight above them: dirt holds them up
				BlockState fill = floor.is(Blocks.GRASS_BLOCK) || floor.is(Blocks.DIRT_PATH) || floor.is(Blocks.MYCELIUM) || floor.is(Blocks.PODZOL)
						? Blocks.DIRT.defaultBlockState() : floor;
				int depth = 0;
				pos.move(0, -1, 0);
				while (depth < MAX_DEPTH && hollow(level.getBlockState(pos))) {
					pos.move(0, -1, 0);
					depth++;
				}
				if (depth == 0 || depth == MAX_DEPTH && hollow(level.getBlockState(pos))) {
					continue;
				}
				for (int y = house.minY() - 1; y > pos.getY(); y--) {
					level.setBlock(new BlockPos(x, y, z), fill, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** Air, water or a plant: nothing a house rests on. */
	private static boolean hollow(BlockState state) {
		return state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced();
	}
}
