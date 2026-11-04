package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;

import com.cassonade.client.ambience.Birds;

/**
 * Monsters see through the water no better than the player: a drowned 22 blocks away does not notice a player under
 * water, one 10 blocks away does; on land, the same player stays fully visible.
 */
public class MurkyWaterClientGameTest implements FabricClientGameTest {
	private static final int SURFACE = 120;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("forceload add -8 -8 40 8");
			world.getServer().runCommand("fill -4 " + (SURFACE - 12) + " -8 36 " + SURFACE + " 8 glass hollow");
			world.getServer().runCommand("fill -3 " + (SURFACE - 11) + " -7 35 " + SURFACE + " 7 water");
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("effect give @a water_breathing infinite 0 true");
			world.getServer().runCommand("effect give @a resistance infinite 4 true");
			world.getServer().runCommand("tp @a 0.5 " + (SURFACE - 8) + " 0.5 -90 0");
			world.getConnection().waitForChunksRender();
			context.waitTicks(10);

			// Seen through the water: 16 of the drowned's 35 blocks
			world.getServer().runCommand("summon drowned 10.5 " + (SURFACE - 8) + " 0.5 {PersistenceRequired:1b,Tags:[\"near\"]}");
			world.getServer().runCommand("summon drowned 22.5 " + (SURFACE - 8) + " 0.5 {PersistenceRequired:1b,Tags:[\"far\"]}");
			context.waitTicks(5);
			double underWater = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				return player.getVisibilityPercent(level, drowned(level, "far"));
			});
			check(Math.abs(underWater - 16.0 / 35.0) < 0.01, "a player under water visible at 16 of 35 blocks, found " + underWater);

			// Five seconds for each to look around: the near one gives chase, the far one has seen nothing
			context.waitTicks(100);
			boolean nearChases = world.getServer().computeOnServer(server -> drowned(server.overworld(), "near").getTarget() != null);
			boolean farChases = world.getServer().computeOnServer(server -> drowned(server.overworld(), "far").getTarget() != null);
			check(nearChases, "the drowned 10 blocks away chasing the player");
			check(!farChases, "the drowned 22 blocks away not noticing the player through the water");

			// Out of the water, the player is as visible as ever
			world.getServer().runCommand("tp @a 0.5 " + (SURFACE + 2) + " 0.5");
			world.getServer().runCommand("summon zombie 20.5 " + (SURFACE + 2) + " 0.5 {PersistenceRequired:1b,NoAI:1b,Tags:[\"land\"]}");
			context.waitTicks(5);
			double onLand = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Zombie zombie = level.getEntitiesOfClass(Zombie.class, new AABB(-8, 0, -8, 40, 320, 8), mob -> mob.entityTags().contains("land")).getFirst();
				return player.getVisibilityPercent(level, zombie);
			});
			check(onLand == 1.0, "a player out of the water fully visible to a mob out of it, found " + onLand);
		}
	}

	private static Drowned drowned(ServerLevel level, String tag) {
		List<Drowned> found = level.getEntitiesOfClass(Drowned.class, new AABB(-8, 0, -8, 40, 320, 8), drowned -> drowned.entityTags().contains(tag));
		if (found.isEmpty()) {
			throw new AssertionError("Expected the drowned tagged " + tag);
		}
		return found.getFirst();
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
