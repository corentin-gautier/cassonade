package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.inventory.AbstractFurnaceMenu;

import com.cassonade.client.ContainerPanels;

/** A furnace's three slots come down from under the title with the drawing of the window. Only the client draws slots where they are. */
@Mixin(AbstractFurnaceMenu.class)
abstract class AbstractFurnaceMenuMixin {
	@ModifyConstant(method = "<init>(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/inventory/RecipeBookType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V", constant = {@Constant(intValue = 17), @Constant(intValue = 53), @Constant(intValue = 35)})
	private int cassonade$lowerSlot(int y) {
		return y + ContainerPanels.TITLE_ROOM;
	}
}
