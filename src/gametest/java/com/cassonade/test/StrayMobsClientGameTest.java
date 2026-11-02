package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;

import com.cassonade.tweaks.BeeWandering;

/** Iron golems plan their way around fire, and bees stay away from the open sea or come back from it. */
public class StrayMobsClientGameTest implements FabricClientGameTest {
	/** The sea starts this far east of the player. */
	private static final int SHORE = 8;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("time set 6000");
			int[] beeIds = new int[2];
			int[] golemId = new int[1];
			BlockPos[] origin = new BlockPos[1];
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
				BlockPos feet = server.getPlayerList().getPlayers().getFirst().blockPosition();
				origin[0] = feet;

				// A line of fire between a golem and where it is sent
				BlockPos golemAt = feet.west(40);
				for (int x = -12; x <= 12; x++) {
					level.setBlockAndUpdate(golemAt.offset(x, -1, 5), Blocks.NETHERRACK.defaultBlockState());
					level.setBlockAndUpdate(golemAt.offset(x, 0, 5), Blocks.FIRE.defaultBlockState());
				}
				IronGolem golem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
				golem.snapTo(golemAt, 0, 0);
				level.addFreshEntity(golem);
				golemId[0] = golem.getId();
			});
			// A mob plans no way until it stands on the ground
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
				IronGolem golem = (IronGolem) level.getEntity(golemId[0]);
				Path path = golem.getNavigation().createPath(origin[0].west(40).south(10), 0);
				check(path != null, "the golem finds a way");
				for (int i = 0; i < path.getNodeCount(); i++) {
					check(!level.getBlockState(path.getNode(i).asBlockPos()).is(Blocks.FIRE), "the golem's way avoids the fire");
				}
				golem.discard();
			});

			// Open sea to the east, in an ocean biome
			// In strips: the command refuses too large a volume at once
			for (int z = -40; z < 40; z += 16) {
				world.getServer().runCommand("execute as @a at @s run fillbiome ~" + SHORE + " ~-2 ~" + z + " ~90 ~1 ~" + (z + 15) + " minecraft:ocean");
			}
			world.getServer().runCommand("execute as @a at @s run fill ~" + SHORE + " ~-1 ~-40 ~90 ~-1 ~40 minecraft:water");
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
				check(BeeWandering.isOverSea(level, origin[0].east(SHORE + 2)), "the water counts as sea");
				check(!BeeWandering.isOverSea(level, origin[0].east(SHORE - 2)), "the shore does not");
				beeIds[0] = bee(level, origin[0].offset(SHORE - 3, 3, 0));
				beeIds[1] = bee(level, origin[0].offset(SHORE + 24, 3, 0));
			});
			int[] farthest = {0};
			for (int i = 0; i < 40; i++) {
				context.waitTicks(20);
				world.getServer().runOnServer(server -> {
					ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
					farthest[0] = Math.max(farthest[0], level.getEntity(beeIds[0]).blockPosition().getX() - origin[0].getX() - SHORE);
				});
			}
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
				int lost = level.getEntity(beeIds[1]).blockPosition().getX() - origin[0].getX() - SHORE;
				check(farthest[0] <= 3, "the bee ashore stays there, not " + farthest[0] + " blocks out");
				check(lost <= 3, "the bee at sea came back, still " + lost + " blocks out");
			});
		}
	}

	private static int bee(ServerLevel level, BlockPos pos) {
		Bee bee = EntityTypes.BEE.create(level, EntitySpawnReason.COMMAND);
		bee.snapTo(pos, 0, 0);
		bee.setPersistenceRequired();
		level.addFreshEntity(bee);
		return bee.getId();
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
