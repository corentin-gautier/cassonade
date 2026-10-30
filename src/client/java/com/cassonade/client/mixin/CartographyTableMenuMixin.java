package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.inventory.CartographyTableMenu;

import com.cassonade.client.ContainerPanels;

/** The table's three slots move down with the room made under the title (CartographyTableScreenMixin). Only the client draws slots where they are. */
@Mixin(CartographyTableMenu.class)
abstract class CartographyTableMenuMixin {
	/** The heights of the map's slot, of the second item's and of the result; the map slot's first 15 is its column. */
	@ModifyConstant(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			constant = {@Constant(intValue = 15, ordinal = 1), @Constant(intValue = 52), @Constant(intValue = 39)})
	private int cassonade$lowerSlot(int y) {
		return y + ContainerPanels.CARTOGRAPHY_ROOM;
	}
}
