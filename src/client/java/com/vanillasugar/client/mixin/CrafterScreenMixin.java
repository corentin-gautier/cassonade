package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.gui.screens.inventory.CrafterScreen;

import com.vanillasugar.client.ContainerPanels;

/** The crafter's redstone arrow, placed from the screen's middle, rises with the top of the taller window (ContainerPanels). */
@Mixin(CrafterScreen.class)
public abstract class CrafterScreenMixin {
	@ModifyVariable(method = "extractRedstone", at = @At("STORE"), ordinal = 1)
	private int vanillasugar$followWindow(int y) {
		return y - ContainerPanels.lift((CrafterScreen) (Object) this);
	}
}
