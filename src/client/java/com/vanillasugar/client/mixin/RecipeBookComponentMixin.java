package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;

import com.vanillasugar.client.BackpackPanel;
import com.vanillasugar.gear.GearLayout;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
	/** The tabs start below the cover's top edge rather than alongside it (tools/recipe_book_textures.py). */
	@Unique
	private static final int TABS_DOWN = 6;

	@Shadow
	@Final
	protected RecipeBookMenu menu;

	// The first int stored is the tabs' x, the second their y
	@ModifyVariable(method = "updateTabs", at = @At("STORE"), ordinal = 1)
	private int vanillasugar$lowerTabs(int y) {
		return y + TABS_DOWN;
	}

	/** The inventory screen asks the recipe book where to sit: with the backpack open, make room for it on the right. */
	@Inject(method = "updateScreenPosition", at = @At("RETURN"), cancellable = true)
	private void vanillasugar$makeRoomForBackpack(int width, int imageWidth, CallbackInfoReturnable<Integer> cir) {
		if (menu instanceof InventoryMenu && BackpackPanel.isOpen()) {
			cir.setReturnValue(cir.getReturnValueI() - GearLayout.PANEL_SHIFT);
		}
	}
}
