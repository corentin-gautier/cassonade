package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.inventory.AbstractContainerMenu;

import com.cassonade.client.ContainerPanels;

/** The inventory's slots move down, below the line between the window's two parts (ContainerPanels). Only the client draws slots where they are. */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerPanelsMenuMixin {
	@ModifyVariable(method = "addStandardInventorySlots", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private int cassonade$lowerInventory(int top) {
		return top + ContainerPanels.shift((AbstractContainerMenu) (Object) this);
	}
}
