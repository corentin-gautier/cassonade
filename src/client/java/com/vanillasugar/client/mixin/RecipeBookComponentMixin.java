package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;

import com.vanillasugar.client.BackpackPanel;
import com.vanillasugar.gear.GearLayout;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
	@Shadow
	@Final
	protected RecipeBookMenu menu;

	/** The inventory screen asks the recipe book where to sit: with the backpack open, make room for it on the right. */
	@Inject(method = "updateScreenPosition", at = @At("RETURN"), cancellable = true)
	private void vanillasugar$makeRoomForBackpack(int width, int imageWidth, CallbackInfoReturnable<Integer> cir) {
		if (menu instanceof InventoryMenu && BackpackPanel.isOpen()) {
			cir.setReturnValue(cir.getReturnValueI() - GearLayout.PANEL_SHIFT);
		}
	}
}
