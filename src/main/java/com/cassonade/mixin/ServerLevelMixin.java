package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.cassonade.weather.SandDrifts;

/** Where the game lays snow, a sandstorm lays sand in the deserts and the badlands. */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
	@Inject(method = "tickPrecipitation", at = @At("TAIL"))
	private void cassonade$driftSand(BlockPos pos, CallbackInfo ci) {
		SandDrifts.tick((ServerLevel) (Object) this, pos);
	}
}
