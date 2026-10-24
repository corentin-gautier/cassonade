package com.vanillasugar.client;

import java.util.Map;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import com.vanillasugar.client.mixin.AbstractContainerMenuAccessor;
import com.vanillasugar.client.mixin.AbstractContainerScreenAccessor;
import com.vanillasugar.hopper.HopperFilter;

/**
 * Container windows drawn in two parts, the block's on top and the player's inventory below
 * (tools/container_panels.py): their textures leave room around the line between the two, so the
 * inventory's slots and label move down, and the window grows, by as much.
 */
public final class ContainerPanels {
	// How far the inventory moves down, as in tools/container_panels.py
	private static final Map<MenuType<?>, Integer> SHIFTS = Map.ofEntries(
			Map.entry(MenuType.CRAFTING, 20),
			Map.entry(MenuType.FURNACE, 20),
			Map.entry(MenuType.BLAST_FURNACE, 20),
			Map.entry(MenuType.SMOKER, 20),
			Map.entry(MenuType.CRAFTER_3x3, 20),
			Map.entry(MenuType.GENERIC_3x3, 20),
			Map.entry(MenuType.ANVIL, 5),
			Map.entry(MenuType.GRINDSTONE, 16),
			Map.entry(MenuType.SMITHING, 6),
			Map.entry(MenuType.STONECUTTER, 18),
			Map.entry(MenuType.ENCHANTMENT, 19),
			Map.entry(MenuType.LOOM, 20),
			Map.entry(MenuType.CARTOGRAPHY_TABLE, 17),
			Map.entry(MenuType.BREWING_STAND, 24),
			Map.entry(MenuType.GENERIC_9x1, 9),
			Map.entry(MenuType.GENERIC_9x2, 9),
			Map.entry(MenuType.GENERIC_9x3, 9),
			Map.entry(MenuType.GENERIC_9x4, 9),
			Map.entry(MenuType.GENERIC_9x5, 9),
			Map.entry(MenuType.GENERIC_9x6, 9),
			Map.entry(MenuType.SHULKER_BOX, 10),
			Map.entry(MenuType.HOPPER, 10),
			Map.entry(HopperFilter.MENU, 10));

	private ContainerPanels() {
	}

	/** How far up the window's top moved as it grew: what the game places from the screen's middle must rise by as much. */
	public static int lift(AbstractContainerScreen<?> screen) {
		AbstractContainerScreenAccessor window = (AbstractContainerScreenAccessor) screen;
		return (screen.height - window.vanillasugar$getImageHeight() + shift(screen.getMenu())) / 2 - window.vanillasugar$getTopPos();
	}

	/** How far the menu's inventory moves down; 0 for the windows left as they are, the player's inventory among them. */
	public static int shift(AbstractContainerMenu menu) {
		MenuType<?> type = ((AbstractContainerMenuAccessor) menu).vanillasugar$getMenuType();
		return type == null ? 0 : SHIFTS.getOrDefault(type, 0);
	}
}
