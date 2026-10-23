package com.vanillasugar.client.storage;

import java.util.function.Consumer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.InventorySounds;
import com.vanillasugar.client.mixin.AbstractContainerScreenAccessor;
import com.vanillasugar.gear.BackpackContainer;
import com.vanillasugar.gear.GearLayout;
import com.vanillasugar.storage.StoragePayload;
import com.vanillasugar.storage.StoragePayload.Action;

/**
 * The small wooden buttons that sort and put items away, right of a container's and the inventory's
 * titles: in chests, barrels, ender chests and shulker boxes, in the survival inventory and on the
 * open backpack. A middle click on a slot sorts the part of the screen it belongs to too.
 */
public final class StorageButtons {
	public static final int SIZE = 11;
	/** From the right edge of the window to the first button, and between two buttons. */
	private static final int RIGHT_MARGIN = 7;
	private static final int GAP = 2;

	private StorageButtons() {
	}

	/** The chest's or shulker box's buttons, and the inventory's sort below them. */
	public static void addToContainerScreen(AbstractContainerScreen<?> screen, Consumer<AbstractWidget> add) {
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
		int right = accessor.vanillasugar$getLeftPos() + accessor.vanillasugar$getImageWidth() - RIGHT_MARGIN;
		int top = accessor.vanillasugar$getTopPos();
		add.accept(button(right, top + 4, 0, "sort", Action.SORT_CONTAINER, "gui.vanillasugar.storage.sort"));
		add.accept(button(right, top + 4, 1, "deposit", Action.DEPOSIT, "gui.vanillasugar.storage.deposit"));
		add.accept(button(right, top + accessor.vanillasugar$getInventoryLabelY() - 2, 0, "sort", Action.SORT_INVENTORY,
				"gui.vanillasugar.storage.sort_inventory"));
	}

	/** The survival inventory's sort, right above its main slots. */
	public static void addToInventory(int leftPos, int topPos, Consumer<AbstractWidget> add) {
		int right = leftPos + GearLayout.INVENTORY_WIDTH - RIGHT_MARGIN;
		add.accept(button(right, topPos + 71, 0, "sort", Action.SORT_INVENTORY, "gui.vanillasugar.storage.sort_inventory"));
	}

	/** The open backpack's sort and deposit, right of its title. */
	public static void addToBackpack(int leftPos, int topPos, Consumer<AbstractWidget> add) {
		int right = leftPos + GearLayout.PANEL_X + GearLayout.PANEL_WIDTH - RIGHT_MARGIN;
		int y = topPos + GearLayout.PANEL_Y + 4;
		add.accept(button(right, y, 0, "sort", Action.SORT_CONTAINER, "gui.vanillasugar.storage.sort"));
		add.accept(button(right, y, 1, "deposit", Action.DEPOSIT, "gui.vanillasugar.storage.deposit"));
	}

	/** The index-th button from the right edge. */
	private static ImageButton button(int right, int y, int index, String icon, Action action, String tooltip) {
		WidgetSprites sprites = new WidgetSprites(VanillaSugar.id("container/storage_" + icon), VanillaSugar.id("container/storage_" + icon + "_highlighted"));
		ImageButton button = new ImageButton(right - SIZE - index * (SIZE + GAP), y, SIZE, SIZE, sprites,
				pressed -> {
					ClientPlayNetworking.send(new StoragePayload(action));
					InventorySounds.stored();
				});
		button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
		return button;
	}

	/** What a middle click on the slot sorts: the inventory, the open container or the backpack; null for other slots. */
	public static Action sortFor(AbstractContainerMenu menu, Slot slot) {
		if (slot.container instanceof Inventory && slot.getContainerSlot() < Inventory.INVENTORY_SIZE) {
			return Action.SORT_INVENTORY;
		}
		if (slot.container instanceof BackpackContainer || (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu) && !(slot.container instanceof Inventory)) {
			return Action.SORT_CONTAINER;
		}
		return null;
	}
}
