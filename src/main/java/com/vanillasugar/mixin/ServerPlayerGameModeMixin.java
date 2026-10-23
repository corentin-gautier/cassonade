package com.vanillasugar.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import com.vanillasugar.storage.HandRefill;

/** The hand is refilled from the inventory once its last item is placed or used, or its tool breaks on a block. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
	@Shadow
	@Final
	protected ServerPlayer player;

	@WrapMethod(method = "useItem")
	private InteractionResult vanillasugar$refillAfterUse(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
			Operation<InteractionResult> original) {
		return HandRefill.around(player, hand, () -> original.call(player, level, stack, hand));
	}

	@WrapMethod(method = "useItemOn")
	private InteractionResult vanillasugar$refillAfterUseOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
			BlockHitResult hit, Operation<InteractionResult> original) {
		return HandRefill.around(player, hand, () -> original.call(player, level, stack, hand, hit));
	}

	@WrapMethod(method = "destroyBlock")
	private boolean vanillasugar$refillAfterBreak(BlockPos pos, Operation<Boolean> original) {
		return HandRefill.around(player, InteractionHand.MAIN_HAND, () -> original.call(pos));
	}
}
