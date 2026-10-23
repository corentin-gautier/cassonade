package com.vanillasugar.storage;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import com.vanillasugar.gear.BackpackContainer;
import com.vanillasugar.item.VanillaSugarItems;

/**
 * The contents of a shulker box or a backpack, shown in its tooltip in the bundle's style instead of
 * vanilla's list of names: the stacks inside and how many of its slots they fill.
 */
public record ContainerPreview(List<ItemStack> items, int slots) implements TooltipComponent {
	public static Optional<ContainerPreview> of(ItemStack stack) {
		int slots;
		if (stack.is(ItemTags.SHULKER_BOXES)) {
			slots = 27;
		} else if (stack.is(VanillaSugarItems.BACKPACK)) {
			slots = BackpackContainer.SIZE;
		} else {
			return Optional.empty();
		}
		// A shulker box still to be filled from a loot table keeps vanilla's "???"
		if (stack.has(DataComponents.CONTAINER_LOOT)) {
			return Optional.empty();
		}
		List<ItemStack> items = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().toList();
		return Optional.of(new ContainerPreview(items, slots));
	}
}
