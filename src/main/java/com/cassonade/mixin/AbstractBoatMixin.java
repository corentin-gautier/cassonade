package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;

import com.cassonade.tweaks.VehicleExit;

@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin {
	@Inject(method = "getDismountLocationForPassenger", at = @At("HEAD"), cancellable = true)
	private void cassonade$exitWhereLooking(LivingEntity passenger, CallbackInfoReturnable<Vec3> info) {
		Vec3 exit = VehicleExit.find((Entity) (Object) this, passenger);
		if (exit != null) {
			info.setReturnValue(exit);
		}
	}
}
