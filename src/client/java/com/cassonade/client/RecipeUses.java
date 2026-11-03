package com.cassonade.client;

import net.minecraft.world.item.ItemStack;

/** The recipe book narrowed to the recipes that use an item (RecipeBookComponentMixin), set with its key over a slot. */
public interface RecipeUses {
	/** Shows the recipes using this item, in every tab; the same item again, or nothing, brings the whole book back. */
	void cassonade$showUses(ItemStack stack);

	/** The item whose uses are shown, or an empty stack. */
	ItemStack cassonade$uses();
}
