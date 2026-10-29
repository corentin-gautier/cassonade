package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.inventory.Slot;

/** The creative inventory tab's slots wrap the survival inventory's, numbered by the menu rather than the inventory. */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
public interface CreativeSlotWrapperAccessor {
	@Accessor("target")
	Slot cassonade$getTarget();
}
