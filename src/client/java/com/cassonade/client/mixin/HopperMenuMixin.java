package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.inventory.HopperMenu;

import com.cassonade.client.ContainerPanels;

/** The row of a hopper or a hopper minecart comes down from under the title. Only the client draws slots where they are. */
@Mixin(HopperMenu.class)
abstract class HopperMenuMixin {
	@ModifyConstant(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V", constant = {@Constant(intValue = 20)})
	private int cassonade$lowerSlot(int y) {
		return y + ContainerPanels.HOPPER_ROOM;
	}
}
