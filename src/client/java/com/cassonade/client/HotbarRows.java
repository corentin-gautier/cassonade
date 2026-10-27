package com.cassonade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;

import com.cassonade.storage.LockedSlots;

/**
 * A key rotates the inventory's rows into the hotbar: the row just above the hotbar comes down into it, the
 * hotbar goes up to the top row, and the other rows move down one. Four presses bring everything back, so the
 * rows hold sets to switch between: tools, blocks to build with, food and torches. A column where a slot is
 * locked stays as it is. Made of the clicks a number key does in the inventory, so it works on any server.
 */
public final class HotbarRows {
	private HotbarRows() {
	}

	public static void rotate(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		// The inventory window's clicks reach the server only while no other window is open
		if (player == null || player.isSpectator() || player.containerMenu != player.inventoryMenu) {
			return;
		}
		LockedSlots locked = LockedSlots.of(player);
		boolean moved = false;
		for (int column = 0; column < Inventory.getSelectionSize(); column++) {
			if (isLocked(locked, column) || isEmpty(player.getInventory(), column)) {
				continue;
			}
			// Swapped with the top row, then the middle one, then the bottom one: each row ends one lower,
			// the hotbar's stack at the top
			for (int row = 1; row <= 3; row++) {
				minecraft.gameMode.handleContainerInput(player.inventoryMenu.containerId, menuSlot(row, column), column, ContainerInput.SWAP, player);
			}
			moved = true;
		}
		if (moved) {
			InventorySounds.rotated();
		}
	}

	/** The inventory window's slot for the inventory's row (1 the top one, 3 the one above the hotbar) and column. */
	private static int menuSlot(int row, int column) {
		return InventoryMenu.INV_SLOT_START + (row - 1) * Inventory.getSelectionSize() + column;
	}

	private static boolean isLocked(LockedSlots locked, int column) {
		for (int row = 0; row <= 3; row++) {
			if (locked.isLocked(row * Inventory.getSelectionSize() + column)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isEmpty(Inventory inventory, int column) {
		for (int row = 0; row <= 3; row++) {
			if (!inventory.getItem(row * Inventory.getSelectionSize() + column).isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
