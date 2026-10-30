package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.inventory.CraftingMenu;

import com.cassonade.client.ContainerPanels;

/** The grid and the result come down from under the title with the drawing of the window. Only the client draws slots where they are. */
@Mixin(CraftingMenu.class)
abstract class CraftingMenuMixin {
	@ModifyConstant(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			constant = {@Constant(intValue = 17), @Constant(intValue = 35)})
	private int cassonade$lowerSlot(int y) {
		return y + ContainerPanels.TITLE_ROOM;
	}
}
