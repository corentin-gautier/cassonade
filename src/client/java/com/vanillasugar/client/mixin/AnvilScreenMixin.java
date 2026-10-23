package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;

/** The anvil shows its cost however high, in red when the player lacks the levels, instead of "Too Expensive!". */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
	@ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40))
	private int vanillasugar$noTooExpensive(int cap) {
		return Integer.MAX_VALUE;
	}
}
