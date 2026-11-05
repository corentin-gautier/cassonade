package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.MorningMist;

/**
 * The morning mist stays as it is along a slope and past a house, up a hill, and only leaves underground.
 */
public class MistClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set " + MistyMorning.sunrise());
			world.getServer().runCommand("gamemode spectator @a");
			// Facing the rising sun from the ground: nothing of the sky or of the distance shows through
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~ ~ -90 0");
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> MorningMist.density() == 1, 20 * 15);
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-mist-horizon");
			// Before dawn, it stays dark
			world.getServer().runCommand("time set " + (MistyMorning.sunrise() - 1400));
			context.waitTicks(40);
			Screenshots.take(context, "cassonade-mist-night");
			world.getServer().runCommand("time set " + MistyMorning.sunrise());
			// In the air, over nothing, at a height any hill has
			world.getServer().runCommand("execute as @p at @s run tp @a ~ 100 ~");
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> MorningMist.density() == 1, 20 * 15);

			// A slope on one side, its samples buried
			world.getServer().runCommand("execute as @p at @s run fill ~-7 ~-2 ~-7 ~7 ~3 ~-3 minecraft:stone");
			context.waitTicks(10);
			checkDensity(context, 1, "the mist doesn't change along a slope");
			world.getServer().runCommand("execute as @p at @s run fill ~-7 ~-2 ~-7 ~7 ~3 ~-3 minecraft:air");

			// A house whose dark inside holds some of the samples
			world.getServer().runCommand("execute as @p at @s run fill ~3 ~-2 ~3 ~7 ~4 ~7 minecraft:stone hollow");
			context.waitTicks(10);
			checkDensity(context, 1, "the mist doesn't change past a house");
			world.getServer().runCommand("execute as @p at @s run fill ~3 ~-2 ~3 ~7 ~4 ~7 minecraft:air");

			// A thick canopy overhead takes the sky's light at the player's block
			world.getServer().runCommand("execute as @p at @s run fill ~-2 ~3 ~-2 ~2 ~9 ~2 minecraft:oak_leaves[persistent=true]");
			context.waitTicks(10);
			checkDensity(context, 1, "the mist doesn't change under a tree");
			world.getServer().runCommand("execute as @p at @s run fill ~-2 ~3 ~-2 ~2 ~9 ~2 minecraft:air");

			// Up high, it thins
			world.getServer().runCommand("execute as @p at @s run tp @a ~ 150 ~");
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> Math.abs(MorningMist.density() - 0.5F) < 0.02F, 20 * 15);

			// A room underground: no sky at any sample that isn't solid
			world.getServer().runCommand("execute as @p at @s run tp @a ~ 100 ~");
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> MorningMist.density() == 1, 20 * 15);
			world.getServer().runCommand("execute as @p at @s run fill ~-8 ~-4 ~-8 ~8 ~8 ~8 minecraft:stone hollow");
			context.waitFor(client -> MorningMist.density() == 0, 20 * 15);

			// Forced, to film it: at its thickest at noon, out of the room
			world.getServer().runCommand("execute as @p at @s run fill ~-8 ~-4 ~-8 ~8 ~8 ~8 minecraft:air");
			world.getServer().runCommand("time set 6000");
			context.runOnClient(client -> MorningMist.setForced(true));
			context.waitFor(client -> MorningMist.density() == 1, 20 * 15);
		} finally {
			context.runOnClient(client -> MorningMist.setForced(false));
		}
	}

	private static void checkDensity(ClientGameTestContext context, float expected, String message) {
		check(context.computeOnClient(client -> MorningMist.density()) == expected, message);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
