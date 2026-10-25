package com.vanillasugar.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.vanillasugar.client.quest.QuestLog;
import com.vanillasugar.client.quest.QuestOptInScreen;

/**
 * Small tweaks, one at a time: a ladder used on a ladder extends the column upward, or downward when
 * looking down, and stops where the wall ends.
 */
public class TweaksClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				// A wall five blocks high, ladders on its two middle blocks, facing west
				BlockPos base = player.blockPosition().east(3).above(10);
				for (int i = 0; i < 5; i++) {
					level.setBlockAndUpdate(base.above(i), Blocks.STONE.defaultBlockState());
					level.setBlockAndUpdate(base.above(i).west(), Blocks.AIR.defaultBlockState());
				}
				level.setBlockAndUpdate(base.above(5).west(), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(base.below().west(), Blocks.AIR.defaultBlockState());
				var ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
				level.setBlockAndUpdate(base.above(2).west(), ladder);
				level.setBlockAndUpdate(base.above(3).west(), ladder);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LADDER, 8));

				// Looking up: the column grows at its top
				player.setXRot(-30);
				use(player, base.above(2).west());
				check(level.getBlockState(base.above(4).west()).is(Blocks.LADDER), "a ladder added at the top of the column");
				check(player.getMainHandItem().getCount() == 7, "the ladder taken from the hand");

				// Looking down: at its bottom, until the wall ends
				player.setXRot(60);
				use(player, base.above(4).west());
				use(player, base.above(4).west());
				use(player, base.above(4).west());
				check(level.getBlockState(base.above(1).west()).is(Blocks.LADDER)
						&& level.getBlockState(base.west()).is(Blocks.LADDER), "ladders added at the bottom of the column");
				check(level.getBlockState(base.below().west()).isAir(), "no ladder past the wall's end");
				check(player.getMainHandItem().getCount() == 5, "only the ladders placed taken");
			});
		}
	}

	private static void use(ServerPlayer player, BlockPos ladder) {
		player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(ladder), Direction.WEST, ladder, false));
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
