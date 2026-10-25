package com.cassonade.client;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import com.cassonade.enchanting.EnchantmentSeedPayload;

/** The enchantments each option of the open enchanting table gives, from the seed the server sends. */
public final class EnchantmentPreview {
	private static @Nullable EnchantmentSeedPayload seed;

	private EnchantmentPreview() {
	}

	public static void receive(EnchantmentSeedPayload received) {
		seed = received;
	}

	/** Empty until the server sent the seed of this menu, and on a server without the mod. */
	public static List<EnchantmentInstance> of(Minecraft minecraft, EnchantmentMenu menu, int option) {
		if (seed == null || seed.containerId() != menu.containerId || menu.costs[option] <= 0) {
			return List.of();
		}
		return EnchantmentSeedPayload.enchantments(minecraft.level.registryAccess(), menu.getSlot(0).getItem(), seed.seed(), option, menu.costs[option]);
	}
}
