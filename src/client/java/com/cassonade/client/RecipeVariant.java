package com.cassonade.client;

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

	void cassonade$setResult(ItemStack result, boolean smelting);

	ItemStack cassonade$result();

	boolean cassonade$smelting();

	boolean cassonade$craftable();

	List<Ingredient> cassonade$ingredients();
}
