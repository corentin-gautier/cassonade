package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.cassonade.storage.ShulkerFilling;

/** The Shulker box is a plain block item, without a class of its own to give it a bundle's clicks and fill bar. */
@Mixin(Item.class)
public abstract class ItemMixin {
	@Inject(method = "overrideStackedOnOther", at = @At("HEAD"), cancellable = true)
	private void cassonade$shulkerOnStack(ItemStack self, Slot slot, ClickAction action, Player player, CallbackInfoReturnable<Boolean> info) {
		if (ShulkerFilling.clickSlot(self, slot, action, player)) {
			info.setReturnValue(true);
		}
	}

	@Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
	private void cassonade$stackOnShulker(ItemStack self, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carried,
			CallbackInfoReturnable<Boolean> info) {
		if (ShulkerFilling.clickBox(self, other, slot, action, player, carried)) {
			info.setReturnValue(true);
		}
	}

	@Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
	private void cassonade$shulkerBar(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
		if (ShulkerFilling.isBarVisible(stack)) {
			info.setReturnValue(true);
		}
	}

	@Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
	private void cassonade$shulkerBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> info) {
		if (ShulkerFilling.isBarVisible(stack)) {
			info.setReturnValue(ShulkerFilling.barWidth(stack));
		}
	}

	@Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
	private void cassonade$shulkerBarColor(ItemStack stack, CallbackInfoReturnable<Integer> info) {
		if (ShulkerFilling.isBarVisible(stack)) {
			info.setReturnValue(ShulkerFilling.barColor(stack));
		}
	}
}
