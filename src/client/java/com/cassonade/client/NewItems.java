package com.cassonade.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * What was just picked up carries a small golden dot in the inventory, in its slot's top right corner, until
 * the cursor passes over it. A slot is marked when it grows with an item the player just picked up: the
 * server says what was picked up and updates the slots separately, in either order, so each waits a second
 * for the other. Client only: the server tells every client what a player picks up, mod or not.
 */
public final class NewItems {
	/** How long, in ticks, a pickup and a slot that grew wait for each other. */
	private static final int WAIT = 20;
	private static final int DOT = 0xFFFFD76A;
	private static final int DOT_EDGE = 0xFF935F00;

	private static final ItemStack[] previous = new ItemStack[Inventory.INVENTORY_SIZE];
	/** The item each marked slot held when marked; null for the slots not marked. */
	private static final ItemStack[] marked = new ItemStack[Inventory.INVENTORY_SIZE];
	private static final List<Waiting> pickedUp = new ArrayList<>();
	private static final List<Waiting> grown = new ArrayList<>();

	private record Waiting(ItemStack stack, int slot, int since) {
	}

	private static int ticks;

	private NewItems() {
	}

	public static void pickedUp(ItemStack stack) {
		pickedUp.add(new Waiting(stack.copyWithCount(1), -1, ticks));
	}

	public static void tick(Minecraft minecraft) {
		ticks++;
		if (minecraft.player == null) {
			clear();
			return;
		}
		Inventory inventory = minecraft.player.getInventory();
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack stack = inventory.getItem(i);
			ItemStack before = previous[i];
			if (!stack.isEmpty() && (before == null || !ItemStack.isSameItemSameComponents(before, stack) || stack.getCount() > before.getCount())) {
				grown.add(new Waiting(stack.copyWithCount(1), i, ticks));
			}
			// A stack used up, moved or swapped is no longer the one marked
			if (marked[i] != null && !ItemStack.isSameItemSameComponents(marked[i], stack)) {
				marked[i] = null;
			}
			previous[i] = stack.copy();
		}
		for (Iterator<Waiting> picked = pickedUp.iterator(); picked.hasNext(); ) {
			Waiting item = picked.next();
			boolean matched = false;
			for (Iterator<Waiting> slots = grown.iterator(); slots.hasNext(); ) {
				Waiting slot = slots.next();
				if (ItemStack.isSameItemSameComponents(item.stack(), slot.stack())) {
					marked[slot.slot()] = slot.stack();
					slots.remove();
					matched = true;
				}
			}
			if (matched) {
				picked.remove();
			}
		}
		pickedUp.removeIf(waiting -> ticks - waiting.since() > WAIT);
		grown.removeIf(waiting -> ticks - waiting.since() > WAIT);
	}

	private static void clear() {
		Arrays.fill(previous, null);
		Arrays.fill(marked, null);
		pickedUp.clear();
		grown.clear();
	}

	/** Whether the inventory slot is marked: for the tests. */
	public static boolean isMarked(int slot) {
		return slot >= 0 && slot < marked.length && marked[slot] != null;
	}

	/** The dot on a marked slot of the inventory, in any window; hovering the slot takes it off. */
	public static void extractSlot(GuiGraphicsExtractor graphics, Slot slot, boolean hovered) {
		if (!(slot.container instanceof Inventory) || !isMarked(slot.getContainerSlot())) {
			return;
		}
		if (hovered) {
			marked[slot.getContainerSlot()] = null;
			return;
		}
		// A round dot of three pixels across, darker on its edge, in the corner the count and the padlock leave free
		int x = slot.x + 13;
		int y = slot.y;
		graphics.fill(x, y + 1, x + 3, y + 2, DOT_EDGE);
		graphics.fill(x + 1, y, x + 2, y + 3, DOT_EDGE);
		graphics.fill(x + 1, y + 1, x + 2, y + 2, DOT);
	}
}
