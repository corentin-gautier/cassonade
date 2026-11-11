package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * No trigger sound and no screen distortion while standing in a portal. Eyes get used to the water in 3 seconds rather than
 * vanilla's 30, along the same curve.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	private static final float WATER_VISION_SPEED = 10;

	@Shadow
	private int waterVisionTime;

	@Inject(method = "getWaterVision", at = @At("HEAD"), cancellable = true)
	private void cassonade$quickWaterVision(CallbackInfoReturnable<Float> cir) {
		if (!((LocalPlayer) (Object) this).isEyeInFluid(FluidTags.WATER) || !Settings.on(Setting.UNDERWATER_VIEW)) {
			return;
		}
		// Vanilla's counter still runs from 0 to 600 at a tick a step; only how fast the curve reads it changes
		float time = Math.min(waterVisionTime * WATER_VISION_SPEED, 600);
		float first = Mth.clamp(time / 100, 0, 1);
		float second = time < 100 ? 0 : Mth.clamp((time - 100) / 500, 0, 1);
		cir.setReturnValue(first * 0.6F + second * 0.4F);
	}

	@Inject(method = "handlePortalTransitionEffect", at = @At("HEAD"), cancellable = true)
	private void cassonade$noPortalEffect(boolean active, CallbackInfo ci) {
		if (!Settings.on(Setting.INSTANT_PORTAL)) {
			return;
		}
		LocalPlayer player = (LocalPlayer) (Object) this;
		player.oPortalEffectIntensity = 0.0F;
		player.portalEffectIntensity = 0.0F;
		if (player.portalProcess != null) {
			player.portalProcess.setAsInsidePortalThisTick(false);
		}
		ci.cancel();
	}
}
