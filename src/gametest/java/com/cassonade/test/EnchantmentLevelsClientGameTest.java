package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;

/** Every enchantment tops out at I, III or V: those at II go to III, those at IV to V. */
public class EnchantmentLevelsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runOnServer(server -> {
				var enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
				enchantments.listElements().forEach(enchantment -> {
					int max = enchantment.value().getMaxLevel();
					check(max == 1 || max == 3 || max == 5, enchantment.key().identifier() + " tops out at " + max);
				});
				check(enchantments.getOrThrow(Enchantments.PROTECTION).value().getMaxLevel() == 5, "Protection V");
				check(enchantments.getOrThrow(Enchantments.KNOCKBACK).value().getMaxLevel() == 3, "Knockback III");
			});
			// The client gets the server's enchantments, so its tooltips and the enchanting table agree
			check(context.computeOnClient(client -> client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
					.getOrThrow(Enchantments.PUNCH).value().getMaxLevel()) == 3, "Punch III on the client");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
