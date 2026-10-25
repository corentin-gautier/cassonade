package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import com.cassonade.client.combat.DamageDirection;

/** The local player's hits, with where they came from, for DamageDirection. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@WrapOperation(method = "handleDamageEvent", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;handleDamageEvent(Lnet/minecraft/world/damagesource/DamageSource;)V"))
	private void cassonade$damageDirection(Entity entity, DamageSource source, Operation<Void> original) {
		original.call(entity, source);
		if (entity instanceof LocalPlayer player) {
			DamageDirection.hurt(player, source);
		}
	}
}
