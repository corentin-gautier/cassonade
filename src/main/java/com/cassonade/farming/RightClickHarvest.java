package com.cassonade.farming;

import java.util.List;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import com.cassonade.ServerSupport;

/**
 * A right click on a ripe crop harvests it and plants it again: wheat, carrots, potatoes, beetroots,
 * nether wart and cocoa. It drops what breaking it would, tool enchantments included, less the seed
 * that goes back in the ground. Sneaking keeps the usual right click.
 */
public final class RightClickHarvest {
	private RightClickHarvest() {
	}

	public static void init() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || player.isSecondaryUseActive() || player.isSpectator() || !player.mayBuild()
					|| !ServerSupport.has(player)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			BlockState replanted = replanted(state);
			if (replanted == null) {
				return InteractionResult.PASS;
			}
			// The client only answers, so the arm swings and the click goes to the server
			if (level instanceof ServerLevel serverLevel) {
				ItemStack seed = state.getCloneItemStack(level, pos, false);
				List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, null, player, player.getMainHandItem());
				boolean seedKept = false;
				for (ItemStack drop : drops) {
					if (!seedKept && ItemStack.isSameItem(drop, seed)) {
						drop.shrink(1);
						seedKept = true;
					}
					Block.popResource(level, pos, drop);
				}
				level.setBlock(pos, replanted, Block.UPDATE_ALL);
				level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1, 1);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** The crop planted again, or null when it is not a ripe crop. */
	private static BlockState replanted(BlockState state) {
		Block block = state.getBlock();
		// The torchflower turns into a flower when grown, it has no ripe state to harvest
		if (block instanceof CropBlock crop && !(block instanceof TorchflowerCropBlock) && crop.isMaxAge(state)) {
			return crop.getStateForAge(0);
		}
		if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE) {
			return state.setValue(NetherWartBlock.AGE, 0);
		}
		if (block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE) {
			return state.setValue(CocoaBlock.AGE, 0);
		}
		return null;
	}
}
