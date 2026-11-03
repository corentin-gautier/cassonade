package com.cassonade.test.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;

/** What the recipe book's pages show, for the tests. */
@Mixin(RecipeBookPage.class)
public interface RecipeBookPageAccessor {
	@Accessor("recipeCollections")
	List<RecipeCollection> cassonade$getRecipeCollections();
}
