package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import com.vanillasugar.client.BackpackPanel;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
	/** Clicks on the backpack panel must not count as clicks outside the window, which would drop the carried item. */
	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void vanillasugar$insideBackpack(double mx, double my, int xo, int yo, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof InventoryScreen && BackpackPanel.contains(mx, my, xo, yo)) {
			cir.setReturnValue(false);
		}
	}
}
