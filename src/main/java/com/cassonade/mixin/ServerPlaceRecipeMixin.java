package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.cassonade.gear.BackpackContainer;
import com.cassonade.gear.OrganizedBackpack;

/** Once the inventory has none of an ingredient left, the recipe book takes it from an organized backpack. */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {
	@Shadow
	@Final
	private Inventory inventory;

	@Inject(method = "moveItemToGrid", at = @At("HEAD"), cancellable = true)
	private void cassonade$fromBackpack(Slot target, Holder<Item> item, int count, CallbackInfoReturnable<Integer> info) {
		BackpackContainer backpack = OrganizedBackpack.of(inventory.player);
		ItemStack existing = target.getItem();
		if (backpack == null || inventory.findSlotMatchingCraftingIngredient(item, existing) != -1) {
			return;
		}
		ItemStack taken = OrganizedBackpack.takeIngredient(backpack, item, existing, count);
		if (taken.isEmpty()) {
			return;
		}
		int takenCount = taken.getCount();
		if (existing.isEmpty()) {
			target.set(taken);
		} else {
			existing.grow(takenCount);
		}
		info.setReturnValue(count - takenCount);
	}
}
