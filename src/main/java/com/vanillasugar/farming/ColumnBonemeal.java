package com.vanillasugar.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bone meal on sugar cane or a cactus grows its column by one block, up to the three blocks it reaches by itself. Shared by
 * SugarCaneBlockMixin and CactusBlockMixin, which make these blocks bonemealable, by hand as from a dispenser.
 */
public final class ColumnBonemeal {
	private static final int MAX_HEIGHT = 3;

	private ColumnBonemeal() {
	}

	public static boolean canGrow(LevelReader level, BlockPos pos, BlockState state) {
		BlockPos top = top(level, pos, state);
		BlockPos bottom = pos;
		while (level.getBlockState(bottom.below()).is(state.getBlock())) {
			bottom = bottom.below();
		}
		BlockPos above = top.above();
		return top.getY() - bottom.getY() + 1 < MAX_HEIGHT && level.isEmptyBlock(above)
				&& state.getBlock().defaultBlockState().canSurvive(level, above);
	}

	public static void grow(ServerLevel level, BlockPos pos, BlockState state) {
		BlockPos above = top(level, pos, state).above();
		BlockState grown = state.getBlock().defaultBlockState();
		level.setBlockAndUpdate(above, grown);
		level.neighborChanged(grown, above, state.getBlock(), null, false);
	}

	private static BlockPos top(LevelReader level, BlockPos pos, BlockState state) {
		BlockPos top = pos;
		while (level.getBlockState(top.above()).is(state.getBlock())) {
			top = top.above();
		}
		return top;
	}
}
