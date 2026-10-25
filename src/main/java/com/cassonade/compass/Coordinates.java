package com.cassonade.compass;

import net.minecraft.core.BlockPos;

/** Coordinates as shown on the maps and in messages, each number named by its axis. */
public final class Coordinates {
	private Coordinates() {
	}

	/** "X 12, Y 64, Z -30" */
	public static String of(BlockPos pos) {
		return "X " + pos.getX() + ", Y " + pos.getY() + ", Z " + pos.getZ();
	}

	/** "X 12, Z -30", for a column without height. */
	public static String of(int x, int z) {
		return "X " + x + ", Z " + z;
	}
}
