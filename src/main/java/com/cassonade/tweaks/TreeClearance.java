package com.cassonade.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;

/**
 * Trees grow two blocks taller so the canopy sits above a player's head, except one in eight that
 * stays vanilla height. Leaves are still refused in those two blocks, for hanging cherry and pale
 * oak that would otherwise dip back down. Bushes are left alone.
 */
public final class TreeClearance {
	public static final int EXTRA_TRUNK = 2;
	public static final int SMALL_TREE_CHANCE = 8;
	public static final int AIR_UNDER_LEAVES = 2;

	private static final ThreadLocal<State> GROWING = new ThreadLocal<>();

	private TreeClearance() {
	}

	public static void begin(BlockPos origin, boolean skip) {
		GROWING.set(new State(origin.immutable(), skip));
	}

	public static void end() {
		GROWING.remove();
	}

	public static int adjustHeight(int height, RandomSource random) {
		State growing = GROWING.get();
		if (growing == null || growing.skip || random.nextInt(SMALL_TREE_CHANCE) == 0) {
			return height;
		}
		return Math.min(TrunkPlacer.MAX_HEIGHT, height + EXTRA_TRUNK);
	}

	public static boolean allowLeaf(BlockPos pos) {
		State growing = GROWING.get();
		if (growing == null || growing.skip) {
			return true;
		}
		return pos.getY() >= growing.origin.getY() + AIR_UNDER_LEAVES;
	}

	private record State(BlockPos origin, boolean skip) {
	}
}
