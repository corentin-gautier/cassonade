package com.vanillasugar.gear;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

public class GearMenuSlot extends Slot {
	private final GearSlot type;

	public GearMenuSlot(GearContainer container, GearSlot type) {
		super(container, type.ordinal(), GearLayout.GEAR_X, GearLayout.gearSlotY(type.ordinal()));
		this.type = type;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return type.accepts(stack);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public Identifier getNoItemIcon() {
		return type.emptyIcon();
	}
}
