package com.cassonade.woodcutter;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The stonecutter's block, with the woodcutter's window. */
public class WoodcutterBlock extends StonecutterBlock {
	private static final Component CONTAINER_TITLE = Component.translatable("container.cassonade.woodcutter");

	public WoodcutterBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider(
				(containerId, inventory, player) -> new WoodcutterMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
				CONTAINER_TITLE);
	}
}
