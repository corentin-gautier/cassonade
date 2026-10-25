package com.cassonade.gear;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

/** A backpack slot, only shown while the backpack panel is open and usable while a backpack is worn. */
public class BackpackMenuSlot extends Slot {
	private final BackpackContainer backpack;

	public BackpackMenuSlot(BackpackContainer backpack, int index) {
		super(backpack, index, GearLayout.backpackSlotX(index), GearLayout.backpackSlotY(index));
		this.backpack = backpack;
	}

	@Override
	public boolean isActive() {
		return backpack.isOpen();
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return backpack.canPlaceItem(getContainerSlot(), stack);
	}

	@Override
	public boolean mayPickup(Player player) {
		return backpack.isWorn();
	}
}
