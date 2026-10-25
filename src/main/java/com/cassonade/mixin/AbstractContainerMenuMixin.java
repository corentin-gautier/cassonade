package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.cassonade.storage.LockedSlots;

/** A shift click does not fill the player's locked slots when they are empty; their stacks still take more. */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
	@WrapOperation(method = "moveItemStackTo", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean cassonade$notIntoLocked(Slot slot, ItemStack stack, Operation<Boolean> original) {
		return !LockedSlots.isLocked(slot) && original.call(slot, stack);
	}
}
