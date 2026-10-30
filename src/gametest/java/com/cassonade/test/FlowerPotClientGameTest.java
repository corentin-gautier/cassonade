package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class FlowerPotClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("execute at @a run setblock ~2 ~ ~ potted_dandelion");
			world.getServer().runCommand("item replace entity @a hotbar.0 with poppy 1");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getInventory().setSelectedSlot(0);
				BlockPos pot = player.blockPosition().east(2);

				use(player, pot);
				check(player.level().getBlockState(pot).is(Blocks.POTTED_POPPY), "the poppy in the pot");
				check(player.getMainHandItem().is(Items.DANDELION) && player.getMainHandItem().getCount() == 1, "the dandelion back in the hand");

				player.getInventory().setItem(0, Items.POPPY.getDefaultInstance());
				use(player, pot);
				check(player.level().getBlockState(pot).is(Blocks.POTTED_POPPY) && player.getMainHandItem().is(Items.POPPY), "the same plant changes nothing");

				player.getInventory().setItem(0, Items.COBBLESTONE.getDefaultInstance());
				use(player, pot);
				check(player.level().getBlockState(pot).is(Blocks.FLOWER_POT) && player.getInventory().contains(Items.POPPY.getDefaultInstance()), "anything else empties the pot, as in vanilla");
			});
		}
	}

	private static void use(ServerPlayer player, BlockPos pot) {
		player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pot), Direction.UP, pot, false));
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
