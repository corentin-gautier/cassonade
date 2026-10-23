package com.vanillasugar.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.gear.BackpackContainer;
import com.vanillasugar.gear.BackpackMenu;

/**
 * Sent by the storage buttons and by a middle click on a slot: sorts a container or the player's
 * inventory, or puts the inventory's items into the open container when it already holds some.
 */
public record StoragePayload(Action action) implements CustomPacketPayload {
	public enum Action {
		/** Sorts the open chest, barrel, shulker box or ender chest, or the open backpack. */
		SORT_CONTAINER,
		/** Sorts the inventory's 27 slots above the hotbar. */
		SORT_INVENTORY,
		/** Puts the inventory's items into the open container when it already holds the same item. */
		DEPOSIT
	}

	public static final Type<StoragePayload> TYPE = new Type<>(VanillaSugar.id("storage"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StoragePayload> STREAM_CODEC =
			ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal).<RegistryFriendlyByteBuf>cast().map(StoragePayload::new, StoragePayload::action);
	/** By item, then by name for the same item with different components, fullest first. */
	private static final Comparator<ItemStack> ORDER = Comparator
			.comparingInt((ItemStack stack) -> BuiltInRegistries.ITEM.getId(stack.getItem()))
			.thenComparing(stack -> stack.getHoverName().getString())
			.thenComparing(ItemStack::getCount, Comparator.reverseOrder());

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> payload.apply(context.player()));
	}

	private void apply(ServerPlayer player) {
		AbstractContainerMenu menu = player.containerMenu;
		Inventory inventory = player.getInventory();
		switch (action) {
			case SORT_CONTAINER -> {
				Container container = openContainer(menu);
				if (container != null) {
					sort(container, 0, container.getContainerSize());
				}
			}
			case SORT_INVENTORY -> sort(inventory, Inventory.SELECTION_SIZE, Inventory.INVENTORY_SIZE);
			case DEPOSIT -> {
				Container container = openContainer(menu);
				if (container != null) {
					deposit(inventory, container);
				}
			}
		}
		menu.broadcastChanges();
	}

	/** The container a sort or a deposit works on: only plain storage, never a furnace or a crafter's grid. */
	private static Container openContainer(AbstractContainerMenu menu) {
		if (menu instanceof ChestMenu chest) {
			return chest.getContainer();
		}
		if (menu instanceof ShulkerBoxMenu) {
			return menu.getSlot(0).container;
		}
		if (menu instanceof InventoryMenu && menu instanceof BackpackMenu backpackMenu) {
			BackpackContainer backpack = backpackMenu.vanillasugar$backpack();
			return backpack.isOpen() ? backpack : null;
		}
		return null;
	}

	/** Merges the stacks of the slots from {@code start} to {@code end} and puts them back in order. */
	static void sort(Container container, int start, int end) {
		List<ItemStack> merged = new ArrayList<>();
		for (int slot = start; slot < end; slot++) {
			ItemStack stack = container.removeItemNoUpdate(slot);
			for (ItemStack into : merged) {
				if (stack.isEmpty()) {
					break;
				}
				if (ItemStack.isSameItemSameComponents(into, stack) && into.getCount() < into.getMaxStackSize()) {
					int moved = Math.min(stack.getCount(), into.getMaxStackSize() - into.getCount());
					into.grow(moved);
					stack.shrink(moved);
				}
			}
			if (!stack.isEmpty()) {
				merged.add(stack);
			}
		}
		merged.sort(ORDER);
		for (int i = 0; i < merged.size(); i++) {
			container.setItem(start + i, merged.get(i));
		}
		container.setChanged();
	}

	/**
	 * Moves the inventory's stacks above the hotbar into the container when it holds the same item:
	 * onto its stacks first, then into empty slots.
	 */
	static void deposit(Inventory inventory, Container container) {
		for (int slot = Inventory.SELECTION_SIZE; slot < Inventory.INVENTORY_SIZE; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && contains(container, stack)) {
				insert(container, stack);
			}
		}
		inventory.setChanged();
		container.setChanged();
	}

	private static boolean contains(Container container, ItemStack stack) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (ItemStack.isSameItemSameComponents(container.getItem(slot), stack)) {
				return true;
			}
		}
		return false;
	}

	private static void insert(Container container, ItemStack stack) {
		for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
			ItemStack into = container.getItem(slot);
			if (ItemStack.isSameItemSameComponents(into, stack)) {
				int room = Math.min(into.getMaxStackSize(), container.getMaxStackSize()) - into.getCount();
				int moved = Math.min(stack.getCount(), Math.max(0, room));
				into.grow(moved);
				stack.shrink(moved);
			}
		}
		for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
			if (container.getItem(slot).isEmpty() && container.canPlaceItem(slot, stack)) {
				container.setItem(slot, stack.split(Math.min(stack.getCount(), container.getMaxStackSize(stack))));
			}
		}
	}

	@Override
	public Type<StoragePayload> type() {
		return TYPE;
	}
}
