package com.vanillasugar.gear;

import java.util.function.Predicate;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.item.VanillaSugarItems;

/**
 * The dedicated inventory slots, each unlocking a feature while its item is worn. The instruments
 * showing something on screen come first, in the column above the offhand, then what is worn on the
 * body, along the offhand's row (see {@link GearLayout}).
 */
public enum GearSlot {
	COMPASS("compass", stack -> stack.is(Items.COMPASS)),
	MAP("map", stack -> stack.is(VanillaSugarItems.WORLD_MAP)),
	CLOCK("clock", stack -> stack.is(Items.CLOCK)),
	/** Worn beside a chestplate, see {@link ElytraGear}. */
	ELYTRA("elytra", stack -> stack.has(DataComponents.GLIDER)),
	BACKPACK("backpack", stack -> stack.is(VanillaSugarItems.BACKPACK));

	private final Identifier emptyIcon;
	private final Predicate<ItemStack> accepts;

	GearSlot(String name, Predicate<ItemStack> accepts) {
		this.emptyIcon = VanillaSugar.id("container/slot/" + name);
		this.accepts = accepts;
	}

	public boolean accepts(ItemStack stack) {
		return accepts.test(stack);
	}

	public Identifier emptyIcon() {
		return emptyIcon;
	}

	public static GearSlot forItem(ItemStack stack) {
		for (GearSlot slot : values()) {
			if (slot.accepts(stack)) {
				return slot;
			}
		}
		return null;
	}
}
