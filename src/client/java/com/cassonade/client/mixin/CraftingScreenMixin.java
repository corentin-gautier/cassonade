package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.world.inventory.CraftingMenu;

/** The title centered over the grid: vanilla's starts at the grid's left, however long the name is in the language. */
@Mixin(CraftingScreen.class)
abstract class CraftingScreenMixin extends AbstractRecipeBookScreen<CraftingMenu> {
	@Unique
	private static final int GRID_CENTER = 56;

	private CraftingScreenMixin() {
		super(null, null, null, null);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void cassonade$centerTitle(CallbackInfo ci) {
		titleLabelX = GRID_CENTER - font.width(title) / 2;
	}
}
