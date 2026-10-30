package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.inventory.StonecutterMenu;

import com.cassonade.client.ContainerPanels;

/** The two slots follow the panel's content down (StonecutterScreenMixin). Only the client draws slots where they are. */
@Mixin(StonecutterMenu.class)
abstract class StonecutterMenuSlotsMixin {
	@ModifyConstant(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			constant = @Constant(intValue = 33))
	private int cassonade$lowerSlot(int y) {
		return y + ContainerPanels.STONECUTTER_ROOM;
	}
}
