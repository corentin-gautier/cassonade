package com.cassonade.woodcutter;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.StonecutterMenu;

/** The stonecutter's menu, for the woodcutter's block and its own menu type so that the client knows which one is open. */
public class WoodcutterMenu extends StonecutterMenu {
	private final ContainerLevelAccess access;

	public WoodcutterMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public WoodcutterMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(containerId, inventory, access);
		this.access = access;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, Woodcutter.BLOCK);
	}

	@Override
	public MenuType<?> getType() {
		return Woodcutter.MENU;
	}
}
