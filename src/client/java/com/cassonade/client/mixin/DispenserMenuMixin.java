package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.inventory.DispenserMenu;

import com.cassonade.client.ContainerPanels;

/** The grid of a dispenser or a dropper comes down from under the title with the drawing of the window. Only the client draws slots where they are. */
@Mixin(DispenserMenu.class)
abstract class DispenserMenuMixin {
	@ModifyConstant(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V", constant = {@Constant(intValue = 17)})
	private int cassonade$lowerSlot(int y) {
		return y + ContainerPanels.TITLE_ROOM;
	}
}
