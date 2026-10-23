package com.vanillasugar.storage;

import java.util.function.Supplier;

import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * When a hand's stack runs out as it is used (the last block placed, the last food eaten, a tool broken),
 * the same item from the inventory takes its place. Only the inventory: the backpack is left alone.
 */
public final class HandRefill {
	private HandRefill() {
	}

	/** Runs an action of the hand, then refills it if the action used up its stack. */
	public static <T> T around(Player player, InteractionHand hand, Supplier<T> action) {
		if (player.level().isClientSide()) {
			return action.get();
		}
		ItemStack before = player.getItemInHand(hand).copy();
		int selected = player.getInventory().getSelectedSlot();
		T result = action.get();
		// Not when the action switched to another hotbar slot
		if (!before.isEmpty() && player.getItemInHand(hand).isEmpty()
				&& (hand == InteractionHand.OFF_HAND || player.getInventory().getSelectedSlot() == selected)) {
			refill(player, hand, before);
		}
		return result;
	}

	public static void run(Player player, InteractionHand hand, Runnable action) {
		around(player, hand, () -> {
			action.run();
			return null;
		});
	}

	/** Takes the stack most like the one used up: the same item and components first, a tool worn differently next. */
	private static void refill(Player player, InteractionHand hand, ItemStack used) {
		Inventory inventory = player.getInventory();
		NonNullList<ItemStack> items = inventory.getNonEquipmentItems();
		int handSlot = hand == InteractionHand.MAIN_HAND ? inventory.getSelectedSlot() : -1;
		int found = -1;
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			if (i == handSlot || stack.isEmpty() || !stack.is(used.getItem())) {
				continue;
			}
			if (ItemStack.isSameItemSameComponents(stack, used)) {
				found = i;
				break;
			}
			if (found < 0) {
				found = i;
			}
		}
		if (found >= 0) {
			player.setItemInHand(hand, inventory.removeItemNoUpdate(found));
		}
	}
}
