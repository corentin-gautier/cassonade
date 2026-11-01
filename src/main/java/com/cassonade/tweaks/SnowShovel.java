package com.cassonade.tweaks;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import com.cassonade.ServerSupport;

/** A right click with a shovel takes one layer off snow, for one snowball and one use of the shovel. */
public final class SnowShovel {
	private SnowShovel() {
	}

	public static void init() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack shovel = player.getItemInHand(hand);
			if (!shovel.is(ItemTags.SHOVELS) || player.isSpectator() || !player.mayBuild() || !ServerSupport.has(player)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (!state.is(Blocks.SNOW)) {
				return InteractionResult.PASS;
			}
			// The client only answers, so the arm swings and the click goes to the server
			if (level instanceof ServerLevel) {
				int layers = state.getValue(SnowLayerBlock.LAYERS);
				level.setBlock(pos, layers > 1 ? state.setValue(SnowLayerBlock.LAYERS, layers - 1) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1, 1);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				if (!player.hasInfiniteMaterials()) {
					Block.popResource(level, pos, new ItemStack(Items.SNOWBALL));
				}
				shovel.hurtAndBreak(1, player, hand);
			}
			return InteractionResult.SUCCESS;
		});
	}
}
