package com.cassonade.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;

import com.cassonade.test.TestNotifications;

/** The recipes advancements unlock come with the same banners, and cover the same screenshots. */
@Mixin(ToastManager.class)
abstract class ToastManagerMixin {
	@Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
	private void cassonadeTest$mute(Toast toast, CallbackInfo ci) {
		if (!TestNotifications.shown && toast instanceof RecipeToast) {
			ci.cancel();
		}
	}
}
