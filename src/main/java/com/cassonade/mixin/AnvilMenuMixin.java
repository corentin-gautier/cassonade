package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.inventory.AnvilMenu;

/** No "Too Expensive!": the anvil takes any cost the player can pay (see AnvilScreenMixin for the label). */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	// The third 40 of createResult is the cap; the first is the price of a stack, the second the cap on renaming alone
	@ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 2))
	private int cassonade$noCostCap(int cap) {
		return Integer.MAX_VALUE;
	}
}
