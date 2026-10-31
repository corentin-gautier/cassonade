package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.tabs.MenuTabBar;

/** The selected tab opens onto the screen's background, without vanilla's darker one inside it. */
@Mixin(MenuTabBar.MenuTabButton.class)
abstract class MenuTabButtonMixin {
	@Inject(method = "renderMenuBackground", at = @At("HEAD"), cancellable = true)
	private void cassonade$noBackground(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, CallbackInfo info) {
		info.cancel();
	}
}
