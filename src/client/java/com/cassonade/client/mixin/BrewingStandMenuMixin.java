package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.inventory.BrewingStandMenu;

import com.cassonade.client.ContainerPanels;

/** The five slots come down with the stand's drawing, clear of the title (BrewingStandScreenMixin). Only the client draws slots where they are. */
@Mixin(BrewingStandMenu.class)
abstract class BrewingStandMenuMixin {
	/** The potions' rows, then the ingredient's and the fuel's, whose 17 between them is the fuel's column. */
	@ModifyConstant(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
			constant = {@Constant(intValue = 51), @Constant(intValue = 58), @Constant(intValue = 17, ordinal = 0), @Constant(intValue = 17, ordinal = 2)})
	private int cassonade$moveRow(int y) {
		return y + ContainerPanels.BREWING_DOWN;
	}
}
