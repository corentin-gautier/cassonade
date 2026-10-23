package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.ToastManager;

import com.vanillasugar.client.quest.AchievementBanner;

/** The advancement banner is drawn with the toasts, over the screens. */
@Mixin(ToastManager.class)
public abstract class ToastManagerMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void vanillasugar$drawBanner(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		AchievementBanner.extractRenderState(graphics);
	}
}
