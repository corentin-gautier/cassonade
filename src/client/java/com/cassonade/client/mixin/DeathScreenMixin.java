package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.DeathScreen;

import com.cassonade.client.map.DeathMarker;

/** The respawn and title screen buttons are clickable at once instead of after one second. */
@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {
	@Inject(method = "init", at = @At("HEAD"))
	private void cassonade$showNewDeathMarker(CallbackInfo ci) {
		DeathMarker.onDeath();
	}

	@ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/DeathScreen;setButtonsActive(Z)V"))
	private boolean cassonade$noButtonDelay(boolean isActive) {
		return true;
	}
}
