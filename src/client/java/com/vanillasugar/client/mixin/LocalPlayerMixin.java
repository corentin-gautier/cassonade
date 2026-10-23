package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.LocalPlayer;

/** No trigger sound and no screen distortion while standing in a portal. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Inject(method = "handlePortalTransitionEffect", at = @At("HEAD"), cancellable = true)
	private void vanillasugar$noPortalEffect(boolean active, CallbackInfo ci) {
		LocalPlayer player = (LocalPlayer) (Object) this;
		player.oPortalEffectIntensity = 0.0F;
		player.portalEffectIntensity = 0.0F;
		if (player.portalProcess != null) {
			player.portalProcess.setAsInsidePortalThisTick(false);
		}
		ci.cancel();
	}
}
