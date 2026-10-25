package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.gui.screens.DeathScreen;

/** The respawn and title screen buttons are clickable at once instead of after one second. */
@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {
	@ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/DeathScreen;setButtonsActive(Z)V"))
	private boolean cassonade$noButtonDelay(boolean isActive) {
		return true;
	}
}
