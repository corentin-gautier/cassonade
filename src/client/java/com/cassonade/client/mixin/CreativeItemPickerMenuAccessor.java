package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

/** The row of items at the top of the creative inventory's grid, for the scroll sounds. */
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public interface CreativeItemPickerMenuAccessor {
	@Invoker("getRowIndexForScroll")
	int cassonade$rowIndexForScroll(float scrollOffs);
}
