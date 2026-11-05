package com.cassonade.test;

import java.util.function.Predicate;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Pillager outposts, desert pyramids and igloos hold treasures besides vanilla's loot. */
public class StructureLootClientGameTest implements FabricClientGameTest {
	private static final int CHESTS = 100;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			// Each check counts items vanilla's table never gives, so they come from the treasure pool.
			// Outpost: 1 to 2 rolls, emeralds and diamonds 23 of 55 weight, about 63 expected
			check(world, BuiltInLootTables.PILLAGER_OUTPOST, "pillager outpost", "emeralds or diamonds",
					stack -> stack.is(Items.EMERALD) || stack.is(Items.DIAMOND), 30);
			// Pyramid: 1 roll, gold blocks 10 of 49 weight, about 20 expected
			check(world, BuiltInLootTables.DESERT_PYRAMID, "desert pyramid", "gold blocks",
					stack -> stack.is(Items.GOLD_BLOCK), 8);
			// Igloo: 1 roll, diamonds, splash potions and enchanted books 24 of 39 weight, about 62 expected
			check(world, BuiltInLootTables.IGLOO_CHEST, "igloo", "diamonds, potions or books",
					stack -> stack.is(Items.DIAMOND) || stack.is(Items.SPLASH_POTION) || stack.is(Items.ENCHANTED_BOOK), 30);
		}
	}

	private static void check(TestSingleplayerContext world, ResourceKey<LootTable> key, String name, String what, Predicate<ItemStack> treasure, int min) {
		int found = world.getServer().computeOnServer(server -> {
			ServerLevel level = server.overworld();
			LootTable table = server.reloadableRegistries().getLootTable(key);
			int treasures = 0;
			for (int i = 0; i < CHESTS; i++) {
				LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
				for (ItemStack stack : table.getRandomItems(params)) {
					if (treasure.test(stack)) {
						treasures++;
					}
				}
			}
			return treasures;
		});
		System.out.println("Structure loot: " + found + " " + what + " in " + CHESTS + " " + name + " chests");
		if (found < min) {
			throw new AssertionError("Expected the treasure pool in " + name + " chests, found " + found + " " + what);
		}
	}
}
