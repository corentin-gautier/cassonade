package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.multiplayer.ClientAdvancements;

import com.cassonade.client.quest.AchievementBanner;

/** Advancements made are celebrated at the center of the screen instead of in a toast. */
@Mixin(ClientAdvancements.class)
public abstract class ClientAdvancementsMixin {
	@WrapOperation(method = "update", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;addToast(Lnet/minecraft/client/gui/components/toasts/Toast;)V"))
	private void cassonade$celebrate(ToastManager toasts, Toast toast, Operation<Void> original) {
		if (toast instanceof AdvancementToast advancement) {
			AchievementBanner.add(((AdvancementToastAccessor) advancement).getAdvancement());
		} else {
			original.call(toasts, toast);
		}
	}
}
