package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;

/** Container panels are wood (tools/gui_style.py): their titles, dark grey in vanilla, are written in cream. */
@Mixin({ AbstractContainerScreen.class, InventoryScreen.class, CreativeModeInventoryScreen.class, MerchantScreen.class })
public abstract class ContainerLabelsMixin {
	@Unique
	private static final int CREAM = 0xFFECD9B2;

	@ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 0xFF404040))
	private int cassonade$creamLabel(int color) {
		return CREAM;
	}
}
