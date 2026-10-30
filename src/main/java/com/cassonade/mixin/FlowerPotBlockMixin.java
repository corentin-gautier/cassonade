package com.cassonade.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/** Another plant swaps with the one in the pot. Only on the server: the client keeps vanilla's answer, a server without the mod refusing the swap. */
@Mixin(FlowerPotBlock.class)
public abstract class FlowerPotBlockMixin {
	@Shadow
	@Final
	private static Map<Block, Block> POTTED_BY_CONTENT;
	@Shadow
	@Final
	private Block potted;

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void cassonade$swapPlant(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> info) {
		if (level.isClientSide() || potted == Blocks.AIR || !(itemStack.getItem() instanceof BlockItem blockItem)
				|| blockItem.getBlock() == potted || !POTTED_BY_CONTENT.containsKey(blockItem.getBlock())) {
			return;
		}
		level.setBlockAndUpdate(pos, POTTED_BY_CONTENT.get(blockItem.getBlock()).defaultBlockState());
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		player.awardStat(Stats.POT_FLOWER);
		// Consumed first: the last flower of the hand leaves its slot to the plant coming back
		itemStack.consume(1, player);
		ItemStack plant = new ItemStack(potted);
		if (!player.addItem(plant)) {
			player.drop(plant, false, Prediction.PREDICTED);
		}
		info.setReturnValue(InteractionResult.SUCCESS);
	}
}
