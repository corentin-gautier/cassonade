package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

import com.cassonade.trader.WorkstationPayload;

@Mixin(Villager.class)
public abstract class VillagerMixin {
	@Inject(method = "startTrading", at = @At("TAIL"))
	private void cassonade$sendWorkstation(Player player, CallbackInfo ci) {
		if (player instanceof ServerPlayer serverPlayer) {
			WorkstationPayload.send(serverPlayer, (Villager) (Object) this);
		}
	}
}
