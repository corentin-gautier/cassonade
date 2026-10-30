package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.inventory.GrindstoneMenu;

import com.cassonade.client.ContainerPanels;

/** The three slots move down with the room made under the title (GrindstoneScreenMixin). Only the client draws slots where they are. */
@Mixin(GrindstoneMenu.class)
abstract class GrindstoneMenuMixin {
	@ModifyConstant(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			constant = {@Constant(intValue = 19), @Constant(intValue = 40), @Constant(intValue = 34)})
	private int cassonade$lowerSlot(int y) {
		return y + ContainerPanels.GRINDSTONE_ROOM;
	}
}
