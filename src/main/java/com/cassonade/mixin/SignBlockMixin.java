package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Clicking a sign hung on a chest or a barrel opens the chest, where vanilla would edit the sign: labels no
 * longer get in the way. Sneaking edits the sign as before, dyes, glow ink and honeycomb still apply to it,
 * and a sign with click commands (adventure maps) runs them.
 */
@Mixin(SignBlock.class)
public abstract class SignBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void cassonade$openLabeledChest(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (!(level instanceof ServerLevel) || !(state.getBlock() instanceof WallSignBlock) || player.isSecondaryUseActive()
				|| !(level.getBlockEntity(pos) instanceof SignBlockEntity sign)
				|| sign.getText(sign.getSlotPlayerIsFacing(player)).hasAnyClickCommands(player.isTextFilteringEnabled())) {
			return;
		}
		BlockPos behind = pos.relative(state.getValue(WallSignBlock.FACING).getOpposite());
		BlockState storage = level.getBlockState(behind);
		if (storage.getBlock() instanceof ChestBlock || storage.getBlock() instanceof BarrelBlock) {
			cir.setReturnValue(storage.useWithoutItem(level, player, hit.withPosition(behind)));
		}
	}
}
