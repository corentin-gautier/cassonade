package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;

import com.cassonade.client.ContainerPanels;

/** The recipe book's button, placed from the screen's middle, rises with the top of the taller window (ContainerPanels). */
@Mixin({ CraftingScreen.class, AbstractFurnaceScreen.class })
public abstract class RecipeBookButtonMixin {
	@ModifyReturnValue(method = "getRecipeBookButtonPosition", at = @At("RETURN"))
	private ScreenPosition cassonade$followWindow(ScreenPosition position) {
		return new ScreenPosition(position.x(), position.y() - ContainerPanels.lift((AbstractContainerScreen<?>) (Object) this));
	}
}
