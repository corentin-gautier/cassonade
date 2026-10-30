package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.inventory.CrafterMenu;

import com.cassonade.client.ContainerPanels;

/** The crafter's grid and result come down from under the title with the drawing of the window. Only the client draws slots where they are. */
@Mixin(CrafterMenu.class)
abstract class CrafterMenuMixin {
	@ModifyConstant(method = "addSlots", constant = {@Constant(intValue = 17), @Constant(intValue = 35)})
	private int cassonade$lowerSlot(int y) {
		return y + ContainerPanels.TITLE_ROOM;
	}
}
