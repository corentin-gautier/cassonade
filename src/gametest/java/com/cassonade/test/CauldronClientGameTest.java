package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

public class CauldronClientGameTest implements FabricClientGameTest {
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
				level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));

				washes(player, level, pos, Items.WOOL.pick(DyeColor.RED), Items.WOOL.pick(DyeColor.WHITE));
				washes(player, level, pos, Items.CARPET.pick(DyeColor.BLUE), Items.CARPET.pick(DyeColor.WHITE));
				washes(player, level, pos, Items.BED.pick(DyeColor.GREEN), Items.BED.pick(DyeColor.WHITE));
				washes(player, level, pos, Items.DYED_TERRACOTTA.pick(DyeColor.BLACK), Items.TERRACOTTA);
				washes(player, level, pos, Items.STAINED_GLASS.pick(DyeColor.PINK), Items.GLASS);
				washes(player, level, pos, Items.STAINED_GLASS_PANE.pick(DyeColor.CYAN), Items.GLASS_PANE);

				ItemStack white = new ItemStack(Items.WOOL.pick(DyeColor.WHITE), 4);
				check(CauldronInteractions.WATER.get(white) == CauldronInteraction.DEFAULT,
						"white wool is left alone");
			});
		}
	}

	private static void washes(ServerPlayer player, ServerLevel level, BlockPos pos, Item dyed, Item clean) {
		level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
		ItemStack stack = new ItemStack(dyed, 8);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		CauldronInteractions.WATER.get(stack).interact(level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, stack);
		ItemStack washed = player.getItemInHand(InteractionHand.MAIN_HAND);
		check(washed.is(clean) && washed.getCount() == 8, dyed + " is washed into " + clean + ", the whole stack");
		check(level.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 2, "washing uses one level of water");
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
