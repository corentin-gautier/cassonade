package com.cassonade.gear;

import com.cassonade.ServerSupport;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

public class GearMenuSlot extends Slot {
	private final GearContainer gear;
	private final GearSlot type;

	public GearMenuSlot(GearContainer container, GearSlot type) {
		super(container, type.ordinal(), GearLayout.gearSlotX(type), GearLayout.gearSlotY(type));
		this.gear = container;
		this.type = type;
	}

	/** Hidden on a server without the mod, which has no slot to keep the gear in. */
	@Override
	public boolean isActive() {
		return ServerSupport.has(gear.player());
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
