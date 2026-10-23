package com.vanillasugar.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;

import com.vanillasugar.client.BackpackPanel;
import com.vanillasugar.client.PaperEditBox;
import com.vanillasugar.gear.GearLayout;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
	/** The tabs start below the cover's top edge rather than alongside it (tools/recipe_book_textures.py). */
	@Unique
	private static final int TABS_DOWN = 6;

	@Shadow
	@Final
	private static Component SEARCH_HINT;

	@Shadow
	@Final
	protected RecipeBookMenu menu;

	@Shadow
	private @Nullable EditBox searchBox;

	/** The search field is written on the page, in ink (tools/recipe_book_textures.py). */
	@Inject(method = "initVisuals", at = @At("TAIL"))
	private void vanillasugar$searchOnPaper(CallbackInfo ci) {
		((PaperEditBox) searchBox).vanillasugar$writeOnPaper(SEARCH_HINT);
	}

	// The first int stored is the tabs' x, the second their y
	@ModifyVariable(method = "updateTabs", at = @At("STORE"), ordinal = 1)
	private int vanillasugar$lowerTabs(int y) {
		return y + TABS_DOWN;
	}

	/** The inventory screen asks the recipe book where to sit: with the backpack open, make room for it on the left. */
	@Inject(method = "updateScreenPosition", at = @At("RETURN"), cancellable = true)
	private void vanillasugar$makeRoomForBackpack(int width, int imageWidth, CallbackInfoReturnable<Integer> cir) {
		if (menu instanceof InventoryMenu && BackpackPanel.isOpen()) {
			cir.setReturnValue(cir.getReturnValueI() + GearLayout.PANEL_SHIFT);
		}
	}
}
