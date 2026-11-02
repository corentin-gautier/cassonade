package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;

/** Water is cloudy: the view blurs into it sooner than vanilla's, starting right at the eye. */
@Mixin(WaterFogEnvironment.class)
abstract class WaterFogEnvironmentMixin {
	@Unique
	private static final float CLEAR_REACH = 0.5F;

	@Inject(method = "setupFog", at = @At("TAIL"))
	private void cassonade$cloudy(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
		fog.environmentalStart = 0;
		fog.environmentalEnd *= CLEAR_REACH;
		fog.skyEnd = fog.environmentalEnd;
		fog.cloudEnd = fog.environmentalEnd;
	}
}
