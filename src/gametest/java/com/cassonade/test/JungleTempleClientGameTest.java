package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** A jungle temple's chests hold treasures besides vanilla's bones and rotten flesh. */
public class JungleTempleClientGameTest implements FabricClientGameTest {
	private static final int CHESTS = 100;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			int[] counts = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				LootTable table = server.reloadableRegistries().getLootTable(BuiltInLootTables.JUNGLE_TEMPLE);
				// Gold blocks and golden apples only come from the treasure pool
				int treasures = 0;
				int stacks = 0;
				for (int i = 0; i < CHESTS; i++) {
					LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
					for (ItemStack stack : table.getRandomItems(params)) {
						stacks++;
						if (stack.is(Items.GOLD_BLOCK) || stack.is(Items.GOLDEN_APPLE)) {
							treasures++;
						}
					}
				}
				return new int[] {treasures, stacks};
			});
			System.out.println("Jungle temple: " + counts[0] + " gold blocks or golden apples in " + counts[1] + " stacks over " + CHESTS + " chests");
			// 2 rolls of 61 weight, 16 of them gold blocks and golden apples: about 52 expected
			if (counts[0] < 25) {
				throw new AssertionError("Expected the treasure pool in jungle temple chests, found " + counts[0] + " gold blocks or golden apples");
			}
		}
	}
}
