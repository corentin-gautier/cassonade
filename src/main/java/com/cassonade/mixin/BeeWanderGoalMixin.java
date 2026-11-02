package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.phys.Vec3;

import com.cassonade.tweaks.BeeWandering;

/** Bees don't wander out to sea. */
@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeWanderGoal")
public abstract class BeeWanderGoalMixin {
	@Shadow
	@Final
	private Bee this$0;

	@ModifyReturnValue(method = "findPos", at = @At("RETURN"))
	private Vec3 cassonade$stayAshore(Vec3 picked) {
		return BeeWandering.destination(this.this$0, picked);
	}
}
