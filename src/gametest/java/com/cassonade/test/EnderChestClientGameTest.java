package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** The ender chest's own drop, and the two recipes shipped with it that undo a vanilla block. */
public class EnderChestClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = server.overworld();
				BlockPos pos = player.blockPosition().above(5);
				level.setBlockAndUpdate(pos, Blocks.ENDER_CHEST.defaultBlockState());
				List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos), player, new ItemStack(Items.WOODEN_PICKAXE));
				check(drops.size() == 1 && drops.getFirst().is(Items.ENDER_CHEST) && drops.getFirst().getCount() == 1, "the chest itself, not obsidian (" + drops + ")");

				for (String recipe : List.of("nether_wart_from_block", "ice_from_packed_ice")) {
					check(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("cassonade", recipe))).isPresent(), recipe + " is loaded");
				}
			});
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
