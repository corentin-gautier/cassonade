package com.cassonade.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentPageAccessor {
	@Accessor("recipeBookPage")
	RecipeBookPage cassonade$getRecipeBookPage();
}
