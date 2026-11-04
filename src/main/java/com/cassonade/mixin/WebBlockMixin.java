package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.cassonade.ServerSupport;

/**
 * A cobweb breaks at once, even by hand, and gives its string (PlayerMixin lets any hand harvest it).
 * Without the mod on the server, vanilla: the client would break a web the server keeps.
 */
@Mixin(WebBlock.class)
public abstract class WebBlockMixin extends Block {
	private WebBlockMixin(Properties properties) {
		super(properties);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return ServerSupport.has(player) ? 1.0F : super.getDestroyProgress(state, player, level, pos);
	}
}
