package com.cassonade.mixin;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.context.UseOnContext;

/** An armor stand is placed with arms, as in Bedrock Edition: shears take them off (ArmorStandMixin). */
@Mixin(ArmorStandItem.class)
public abstract class ArmorStandItemMixin {
	@Inject(method = "useOn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
	private void cassonade$withArms(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir, @Local ArmorStand entity) {
		entity.setShowArms(true);
	}
}
