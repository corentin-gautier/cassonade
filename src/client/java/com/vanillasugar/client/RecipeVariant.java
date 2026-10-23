package com.vanillasugar.client;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * A card of the popup listing a recipe's variants, on a right click in the recipe book
 * (OverlayRecipeButtonMixin): what it makes, and its ingredients where they go in the grid.
 */
public interface RecipeVariant {
	record Ingredient(int column, int row, List<ItemStack> stacks) {
		public ItemStack shown(int index) {
			return stacks.get(index % stacks.size());
		}
	}

	void vanillasugar$setResult(ItemStack result, boolean smelting);

	ItemStack vanillasugar$result();

	boolean vanillasugar$smelting();

	boolean vanillasugar$craftable();

	List<Ingredient> vanillasugar$ingredients();
}
