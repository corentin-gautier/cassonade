package com.cassonade.mushroom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A small plant of mushroom fields, growing only on mycelium and podzol, where mushrooms find their ground. */
public class MyceliumPlantBlock extends VegetationBlock {
	private final VoxelShape shape;

	public MyceliumPlantBlock(double width, double height, Properties properties) {
		super(properties);
		shape = Block.column(width, 0, height);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape;
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return grows(state);
	}

	static boolean grows(BlockState ground) {
		return ground.is(Blocks.MYCELIUM) || ground.is(Blocks.PODZOL);
	}
}
