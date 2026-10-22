package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import com.vanillasugar.client.BackpackPanel;
import com.vanillasugar.gear.GearLayout;

@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {
	@Shadow
	@Final
	private AbstractContainerScreen<?> screen;

	/** Status effects are listed right of the inventory: move them past the backpack panel and the tabs. */
	@ModifyVariable(method = "canSeeEffects", at = @At("STORE"), ordinal = 0)
	private int vanillasugar$skipBackpackPanel(int xo) {
		return screen instanceof InventoryScreen ? xo + BackpackPanel.rightEdge() - GearLayout.INVENTORY_WIDTH : xo;
	}

	// The mouse coordinates come first among the int variables here
	@ModifyVariable(method = "extractRenderState", at = @At("STORE"), ordinal = 2)
	private int vanillasugar$drawPastBackpackPanel(int xo) {
		return vanillasugar$skipBackpackPanel(xo);
	}
}
