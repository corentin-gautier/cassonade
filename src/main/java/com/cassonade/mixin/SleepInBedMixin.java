package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.SleepInBed;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;
import com.cassonade.tweaks.PlayerBeds;

/** A villager who claimed a bed before a player slept in it gives it up, and goes looking for another. */
@Mixin(SleepInBed.class)
abstract class SleepInBedMixin {
	@Inject(method = "checkExtraStartConditions", at = @At("HEAD"), cancellable = true)
	private void cassonade$leavePlayerBed(ServerLevel level, LivingEntity body, CallbackInfoReturnable<Boolean> cir) {
		if (body instanceof Villager villager && Settings.on(Setting.PLAYER_BEDS)
				&& villager.getBrain().getMemory(MemoryModuleType.HOME).filter(home -> home.dimension() == level.dimension() && PlayerBeds.isPlayerBed(level, home.pos())).isPresent()) {
			villager.releasePoi(MemoryModuleType.HOME);
			villager.getBrain().eraseMemory(MemoryModuleType.HOME);
			cir.setReturnValue(false);
		}
	}
}
