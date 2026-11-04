package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** A cobweb breaks in one tick by hand and drops its string. */
public class CobwebClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				BlockPos web = player.blockPosition().east(2);
				level.setBlockAndUpdate(web, Blocks.COBWEB.defaultBlockState());
				check(player.getMainHandItem().isEmpty(), "an empty hand");
				check(level.getBlockState(web).getDestroyProgress(player, level, web) >= 1, "the web breaks at once by hand");
				check(player.gameMode.destroyBlock(web), "the web is broken");
				check(level.getBlockState(web).isAir(), "the web is gone");
				check(!level.getEntitiesOfClass(ItemEntity.class, new AABB(web).inflate(1), item -> item.getItem().is(Items.STRING)).isEmpty(),
						"the web drops its string");
			});
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
