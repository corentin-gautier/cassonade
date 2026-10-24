package com.vanillasugar.gear;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Exposes a player's {@link PlayerGear} attachment as a container, one slot per {@link GearSlot}. */
public class GearContainer implements Container {
	private static final GearSlot[] SLOTS = GearSlot.values();

	private final Player player;

	public GearContainer(Player player) {
		this.player = player;
	}

	public Player player() {
		return player;
	}

	@Override
	public int getContainerSize() {
		return SLOTS.length;
	}

	@Override
	public boolean isEmpty() {
		PlayerGear gear = PlayerGear.of(player);
		for (GearSlot slot : SLOTS) {
			if (!gear.get(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return PlayerGear.of(player).get(SLOTS[slot]);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack stack = getItem(slot);
		if (stack.isEmpty() || count <= 0) {
			return ItemStack.EMPTY;
		}

		ItemStack removed = stack.split(count);
		if (stack.isEmpty()) {
			setItem(slot, ItemStack.EMPTY);
		} else {
			setChanged();
		}
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack stack = getItem(slot);
		setItem(slot, ItemStack.EMPTY);
		return stack;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(SLOTS[slot], stack));
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return SLOTS[slot].accepts(stack);
	}

	@Override
	public void setChanged() {
		// Re-set the attachment so in-place stack changes are saved and synced
		player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player));
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.EMPTY);
	}
}
