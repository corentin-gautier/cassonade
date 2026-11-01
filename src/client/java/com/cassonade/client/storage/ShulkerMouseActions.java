package com.cassonade.client.storage;

import org.joml.Vector2i;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.cassonade.storage.ShulkerFilling;

/** The wheel over a Shulker box picks the stack to take out, as vanilla's BundleMouseActions does for a bundle. */
public class ShulkerMouseActions implements ItemSlotMouseAction {
	private final Minecraft minecraft;
	private final ScrollWheelHandler wheel = new ScrollWheelHandler();

	public ShulkerMouseActions(Minecraft minecraft) {
		this.minecraft = minecraft;
	}

	@Override
	public boolean matches(Slot slot) {
		return ShulkerFilling.clicksLikeBundle(slot.getItem(), minecraft.player);
	}

	@Override
	public boolean onMouseScrolled(double scrollX, double scrollY, int slotIndex, ItemStack box) {
		int stacks = ShulkerFilling.stacksIn(box);
		if (stacks == 0) {
			return false;
		}
		Vector2i turned = wheel.onMouseScroll(scrollX, scrollY);
		int steps = turned.y == 0 ? -turned.x : turned.y;
		if (steps != 0) {
			int current = ShulkerFilling.selected(minecraft.player);
			int next = ScrollWheelHandler.getNextScrollWheelSelection(steps, current, stacks);
			if (next != current) {
				select(next);
			}
		}
		return true;
	}

	@Override
	public void onStopHovering(Slot slot) {
		select(-1);
	}

	@Override
	public void onSlotClicked(Slot slot, ContainerInput input) {
		if (input == ContainerInput.QUICK_MOVE || input == ContainerInput.SWAP) {
			select(-1);
		}
	}

	private void select(int index) {
		ShulkerFilling.select(minecraft.player, index);
		ClientPlayNetworking.send(new ShulkerFilling.Select(index));
	}
}
