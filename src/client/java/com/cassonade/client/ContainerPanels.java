package com.cassonade.client;

import java.util.Map;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import com.cassonade.client.mixin.AbstractContainerMenuAccessor;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.hopper.HopperFilter;

/**
 * Container windows drawn in two parts, the block's on top and the player's inventory below
 * (tools/container_panels.py): their textures leave room around the line between the two, so the
 * inventory's slots and label move down, and the window grows, by as much.
 */
public final class ContainerPanels {
	/** Added above the cartography table's content, for its title to breathe. */
	public static final int CARTOGRAPHY_ROOM = 6;
	/** How far the stonecutter's content comes down from under its title, into the room already below it. */
	public static final int STONECUTTER_ROOM = 4;
	/** Added above the grindstone's frame, which vanilla runs under the title. */
	public static final int GRINDSTONE_ROOM = 7;
	/** How far the brewing stand's drawing comes down from under its title, into the room already below it. */
	public static final int BREWING_DOWN = 4;
	/** How far the windows' content comes down from under the title: into the room already below it, but for the crafting table, which grows. */
	public static final int TITLE_ROOM = 5;
	/** Added above a hopper's row, for the same gap under the title: no room is left below it. */
	public static final int HOPPER_ROOM = 2;
	// How far the inventory moves down, as in tools/container_panels.py
	private static final Map<MenuType<?>, Integer> SHIFTS = Map.ofEntries(
			Map.entry(MenuType.CRAFTING, 20 + TITLE_ROOM),
			Map.entry(MenuType.FURNACE, 20),
			Map.entry(MenuType.BLAST_FURNACE, 20),
			Map.entry(MenuType.SMOKER, 20),
			Map.entry(MenuType.CRAFTER_3x3, 20),
			Map.entry(MenuType.GENERIC_3x3, 20),
			Map.entry(MenuType.ANVIL, 5),
			Map.entry(MenuType.GRINDSTONE, 16 + GRINDSTONE_ROOM),
			Map.entry(MenuType.SMITHING, 6),
			Map.entry(MenuType.STONECUTTER, 18),
			Map.entry(MenuType.ENCHANTMENT, 19),
			Map.entry(MenuType.LOOM, 20),
			Map.entry(MenuType.CARTOGRAPHY_TABLE, 17 + CARTOGRAPHY_ROOM),
			Map.entry(MenuType.BREWING_STAND, 24),
			Map.entry(MenuType.GENERIC_9x1, 9),
			Map.entry(MenuType.GENERIC_9x2, 9),
			Map.entry(MenuType.GENERIC_9x3, 9),
			Map.entry(MenuType.GENERIC_9x4, 9),
			Map.entry(MenuType.GENERIC_9x5, 9),
			Map.entry(MenuType.GENERIC_9x6, 9),
			Map.entry(MenuType.SHULKER_BOX, 10),
			Map.entry(MenuType.HOPPER, 10 + HOPPER_ROOM),
			Map.entry(HopperFilter.MENU, 10 + HOPPER_ROOM));

	private ContainerPanels() {
	}

	/** How far up the window's top moved as it grew: what the game places from the screen's middle must rise by as much. */
	public static int lift(AbstractContainerScreen<?> screen) {
		AbstractContainerScreenAccessor window = (AbstractContainerScreenAccessor) screen;
		return (screen.height - window.cassonade$getImageHeight() + shift(screen.getMenu())) / 2 - window.cassonade$getTopPos();
	}

	/** How far the menu's inventory moves down; 0 for the windows left as they are, the player's inventory among them. */
	public static int shift(AbstractContainerMenu menu) {
		MenuType<?> type = ((AbstractContainerMenuAccessor) menu).cassonade$getMenuType();
		return type == null ? 0 : SHIFTS.getOrDefault(type, 0);
	}
}
