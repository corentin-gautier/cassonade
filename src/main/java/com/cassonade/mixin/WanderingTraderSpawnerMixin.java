package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;

import com.cassonade.tweaks.Markets;

/** A wandering trader sent to a town's bell settles at the market's bare stall instead of roaming around the bell. */
@Mixin(WanderingTraderSpawner.class)
abstract class WanderingTraderSpawnerMixin {
	@WrapOperation(method = "spawn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/npc/wanderingtrader/WanderingTrader;setHomeTo(Lnet/minecraft/core/BlockPos;I)V"))
	private void cassonade$settleAtMarket(WanderingTrader trader, BlockPos bell, int radius, Operation<Void> original,
			@Local(argsOnly = true) ServerLevel level) {
		if (!Markets.settle(level, trader, bell)) {
			original.call(trader, bell, radius);
		}
	}
}
