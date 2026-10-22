package com.vanillasugar.gear;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The contents of the backpack worn in the gear slot. Items are cached while the same backpack
 * stack is worn and written back to its {@code container} component on every change.
 *
 * <p>The panel is opened with a button of the inventory screen. Both sides track it (see
 * {@link BackpackTogglePayload}) so that shift-clicks only fill the backpack while it is open.
 */
public class BackpackContainer implements Container {
	/** Twice the 27 slots of the main inventory, like a double chest. */
	public static final int SIZE = 54;

	private final Player player;
	private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private ItemStack loadedFrom = ItemStack.EMPTY;
	private boolean open;

	public BackpackContainer(Player player) {
		this.player = player;
	}

	public boolean isWorn() {
		return PlayerGear.isWearing(player, GearSlot.BACKPACK);
	}

	public boolean isOpen() {
		return open && isWorn();
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	private NonNullList<ItemStack> items() {
		ItemStack backpack = PlayerGear.of(player).backpack();
		if (backpack != loadedFrom) {
			loadedFrom = backpack;
			backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
		}
		return items;
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		return items().stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return items().get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack removed = ContainerHelper.removeItem(items(), slot, count);
		if (!removed.isEmpty()) {
			setChanged();
		}
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack removed = ContainerHelper.takeItem(items(), slot);
		setChanged();
		return removed;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (!isWorn()) {
			return;
		}
		items().set(slot, stack);
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return isWorn() && stack.getItem().canFitInsideContainerItems();
	}

	@Override
	public void setChanged() {
		NonNullList<ItemStack> current = items();
		if (!loadedFrom.isEmpty()) {
			loadedFrom.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(current));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		items().clear();
		setChanged();
	}
}
