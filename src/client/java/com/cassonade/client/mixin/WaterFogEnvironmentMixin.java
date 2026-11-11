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

import com.cassonade.client.dev.DevRendering;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;
import com.cassonade.tweaks.DivingMask;

/** Water is cloudy: the view blurs into it sooner than vanilla's, starting right at the eye, and twice as far through a diving mask. */
@Mixin(WaterFogEnvironment.class)
abstract class WaterFogEnvironmentMixin {
	@Unique
	private static final float CLEAR_REACH = 0.3125F;
	@Unique
	private static final float MASK_REACH = 0.625F;

	@Inject(method = "setupFog", at = @At("TAIL"))
	private void cassonade$cloudy(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (DevRendering.vanilla() || !Settings.on(Setting.UNDERWATER_VIEW)) {
			return;
		}
		fog.environmentalStart = 0;
		fog.environmentalEnd *= DivingMask.worn(camera.entity()) ? MASK_REACH : CLEAR_REACH;
		fog.skyEnd = fog.environmentalEnd;
		fog.cloudEnd = fog.environmentalEnd;
	}
}
