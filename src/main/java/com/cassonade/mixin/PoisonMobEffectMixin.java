package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.effect.PoisonMobEffect;

import com.cassonade.combat.Poison;

/** Poison hurts every 2.5 seconds instead of 1.25 (still twice as often per level), see {@link Poison}. */
@Mixin(PoisonMobEffect.class)
public abstract class PoisonMobEffectMixin {
	@ModifyConstant(method = "shouldApplyEffectTickThisTick", constant = @Constant(intValue = PoisonMobEffect.DAMAGE_INTERVAL))
	private int cassonade$slowerPoison(int interval) {
		return Poison.DAMAGE_INTERVAL;
	}
}
