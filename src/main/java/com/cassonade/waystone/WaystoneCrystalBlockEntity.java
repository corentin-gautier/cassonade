package com.cassonade.waystone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds no data: it is there for the crystal's renderer, as a block model can't move. */
public class WaystoneCrystalBlockEntity extends BlockEntity {
	public WaystoneCrystalBlockEntity(BlockPos pos, BlockState state) {
		super(Waystones.CRYSTAL, pos, state);
	}
}
