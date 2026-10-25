package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.level.levelgen.PhantomSpawner;

import com.cassonade.time.LongDay;

@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {
	/**
	 * Phantoms come for players who skipped three nights, as in vanilla: the time since the last rest is counted
	 * in vanilla days, which last 24000 ticks where the longer days of {@link LongDay} last 36000.
	 */
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"))
	private int cassonade$countVanillaDays(int timeSinceRest) {
		return Math.max(1, (int) ((long) timeSinceRest * LongDay.TICKS_PER_DAY / LongDay.CYCLE_TICKS));
	}
}
