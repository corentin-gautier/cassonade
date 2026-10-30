package com.cassonade.gear;

import java.util.function.Predicate;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.cassonade.Cassonade;
import com.cassonade.item.BackpackItem;
import com.cassonade.item.CassonadeItems;

/**
 * The dedicated inventory slots, each unlocking a feature while its item is worn; an item crafted from
 * the vanilla one goes in the same slot and unlocks more. The instruments showing something on screen come first, in the column above the offhand, then what is worn on the
 * body, along the offhand's row (see {@link GearLayout}).
 */
public enum GearSlot {
	/** The compass, or the sculk compass made from it. */
	COMPASS("compass", stack -> stack.is(Items.COMPASS) || stack.is(CassonadeItems.SCULK_COMPASS)),
	MAP("map", stack -> stack.is(CassonadeItems.WORLD_MAP)),
	/** The clock, or the barometer made from it. */
	CLOCK("clock", stack -> stack.is(Items.CLOCK) || stack.is(CassonadeItems.BAROMETER)),
	/** Worn beside a chestplate, see {@link ElytraGear}. */
	ELYTRA("elytra", stack -> stack.has(DataComponents.GLIDER)),
	BACKPACK("backpack", stack -> stack.getItem() instanceof BackpackItem);

	private final Identifier emptyIcon;
	private final Predicate<ItemStack> accepts;

	GearSlot(String name, Predicate<ItemStack> accepts) {
		this.emptyIcon = Cassonade.id("container/slot/" + name);
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
