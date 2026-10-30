package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HopperMenu;

/** The title centered over the row of slots, itself centered in the window, as a dispenser's over its grid. */
@Mixin(HopperScreen.class)
abstract class HopperScreenMixin extends AbstractContainerScreen<HopperMenu> {
	private HopperScreenMixin(HopperMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
	}
}
