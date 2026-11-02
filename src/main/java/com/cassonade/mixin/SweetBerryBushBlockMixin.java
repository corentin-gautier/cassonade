package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.SweetBerryBushBlock;

/** Thorns don't get through leggings and boots worn together: the bush still slows down, as in vanilla. */
@Mixin(SweetBerryBushBlock.class)
abstract class SweetBerryBushBlockMixin {
	@WrapOperation(method = "entityInside", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean cassonade$stopThorns(Entity entity, ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		if (entity instanceof LivingEntity living
				&& !living.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
				&& !living.getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
			return false;
		}
		return original.call(entity, level, source, damage);
	}
}
