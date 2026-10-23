package com.vanillasugar.hopper;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * The hopper's menu with the filter slot on the left of its five slots. The filter holds a single real item, given back
 * when the hopper is broken. A shift click from the inventory sets the filter while it is empty, then fills the hopper.
 */
public class FilterHopperMenu extends AbstractContainerMenu {
	public static final int FILTER_SLOT = HopperBlockEntity.HOPPER_CONTAINER_SIZE;
	public static final int FILTER_X = 17;
	private static final int INVENTORY_START = FILTER_SLOT + 1;
	private final Container hopper;
	private final Container filter;

	public FilterHopperMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(HopperBlockEntity.HOPPER_CONTAINER_SIZE), new SimpleContainer(1));
	}

	public FilterHopperMenu(int containerId, Inventory inventory, HopperBlockEntity hopper) {
		this(containerId, inventory, hopper, new FilterContainer(hopper));
	}

	private FilterHopperMenu(int containerId, Inventory inventory, Container hopper, Container filter) {
		super(FilterHopper.MENU, containerId);
		this.hopper = hopper;
		this.filter = filter;
		checkContainerSize(hopper, HopperBlockEntity.HOPPER_CONTAINER_SIZE);
		hopper.startOpen(inventory.player);

		for (int x = 0; x < HopperBlockEntity.HOPPER_CONTAINER_SIZE; x++) {
			this.addSlot(new Slot(hopper, x, 44 + x * 18, 20) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return FilterHopper.accepts(filter.getItem(0), stack);
				}
			});
		}
		this.addSlot(new Slot(filter, 0, FILTER_X, 20) {
			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		this.addStandardInventorySlots(inventory, 8, 51);
	}

	@Override
	public boolean stillValid(Player player) {
		return this.hopper.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			if (slotIndex < INVENTORY_START) {
				if (!this.moveItemStackTo(stack, INVENTORY_START, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (this.filter.isEmpty()) {
				if (!this.moveItemStackTo(stack, FILTER_SLOT, FILTER_SLOT + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 0, FILTER_SLOT, false)) {
				return ItemStack.EMPTY;
			}

			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}

		return clicked;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.hopper.stopOpen(player);
	}

	/** The hopper's filter, as a one-slot container for the menu. */
	private record FilterContainer(HopperBlockEntity hopper) implements Container {
		private ItemStack filter() {
			return FilterHopper.filter(this.hopper);
		}

		@Override
		public int getContainerSize() {
			return 1;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return this.filter().isEmpty();
		}

		@Override
		public ItemStack getItem(int slot) {
			return this.filter();
		}

		@Override
		public ItemStack removeItem(int slot, int count) {
			ItemStack filter = this.filter();
			ItemStack removed = filter.split(count);
			this.setItem(slot, filter);
			return removed;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			ItemStack filter = this.filter();
			FilterHopper.setFilter(this.hopper, ItemStack.EMPTY);
			return filter;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			FilterHopper.setFilter(this.hopper, stack);
			this.setChanged();
		}

		@Override
		public void setChanged() {
			this.hopper.setChanged();
		}

		@Override
		public boolean stillValid(Player player) {
			return this.hopper.stillValid(player);
		}

		@Override
		public void clearContent() {
			this.setItem(0, ItemStack.EMPTY);
		}
	}
}
