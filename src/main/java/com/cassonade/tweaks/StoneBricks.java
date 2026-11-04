package com.cassonade.tweaks;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Stone bricks instead of most of the cobblestone of woodland mansions and village houses, some mossy and some cracked as
 * in an old building, keeping the shape of stairs, slabs and walls. Decided by position alone, so a building spanning two
 * chunks gets the same blocks from both.
 */
public final class StoneBricks {
	/** Out of a hundred cobblestone blocks, those turned mossy and cracked, and those left as they are, mended with what was at hand. */
	private static final int MOSSY = 18;
	private static final int CRACKED = 7;
	private static final int KEPT = 12;
	private static final long SALT = 0x5703E;

	private StoneBricks() {
	}

	/** Swaps every cobblestone block of the area. */
	public static void replace(WorldGenLevel level, BoundingBox area) {
		for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
			BlockState stone = of(level.getBlockState(pos), pos);
			if (stone != null) {
				level.setBlock(pos, stone, 2);
			}
		}
	}

	/** The stone bricks taking the place of this cobblestone block, mossy ones for mossy cobblestone: or null for a block left as it is. */
	public static @Nullable BlockState of(BlockState state, BlockPos pos) {
		int roll = RandomSource.create(pos.asLong() * 31 + SALT).nextInt(100);
		if (roll >= MOSSY + CRACKED && roll < MOSSY + CRACKED + KEPT) {
			return null;
		}
		boolean mossy = roll < MOSSY;
		Block block;
		if (state.is(Blocks.COBBLESTONE)) {
			block = mossy ? Blocks.MOSSY_STONE_BRICKS : roll < MOSSY + CRACKED ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS;
		} else if (state.is(Blocks.INFESTED_COBBLESTONE)) {
			block = mossy ? Blocks.INFESTED_MOSSY_STONE_BRICKS : roll < MOSSY + CRACKED ? Blocks.INFESTED_CRACKED_STONE_BRICKS : Blocks.INFESTED_STONE_BRICKS;
		} else if (state.is(Blocks.COBBLESTONE_STAIRS)) {
			block = mossy ? Blocks.MOSSY_STONE_BRICK_STAIRS : Blocks.STONE_BRICK_STAIRS;
		} else if (state.is(Blocks.COBBLESTONE_SLAB)) {
			block = mossy ? Blocks.MOSSY_STONE_BRICK_SLAB : Blocks.STONE_BRICK_SLAB;
		} else if (state.is(Blocks.COBBLESTONE_WALL)) {
			block = mossy ? Blocks.MOSSY_STONE_BRICK_WALL : Blocks.STONE_BRICK_WALL;
		} else if (state.is(Blocks.MOSSY_COBBLESTONE)) {
			block = Blocks.MOSSY_STONE_BRICKS;
		} else if (state.is(Blocks.MOSSY_COBBLESTONE_STAIRS)) {
			block = Blocks.MOSSY_STONE_BRICK_STAIRS;
		} else if (state.is(Blocks.MOSSY_COBBLESTONE_SLAB)) {
			block = Blocks.MOSSY_STONE_BRICK_SLAB;
		} else if (state.is(Blocks.MOSSY_COBBLESTONE_WALL)) {
			block = Blocks.MOSSY_STONE_BRICK_WALL;
		} else {
			return null;
		}
		return block.withPropertiesOf(state);
	}
}
