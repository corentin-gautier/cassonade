package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import com.cassonade.tweaks.MurkyWater;

/** A mob loses track of its target sooner through the water, see {@link MurkyWater}. */
@Mixin(TargetGoal.class)
abstract class TargetGoalMixin {
	@Shadow
	@Final
	protected Mob mob;

	@Inject(method = "getFollowDistance", at = @At("RETURN"), cancellable = true)
	private void cassonade$murkyWater(CallbackInfoReturnable<Double> cir) {
		cir.setReturnValue(MurkyWater.followDistance(this.mob, cir.getReturnValue()));
	}
}
