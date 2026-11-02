package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.cassonade.client.ambience.Aurora;
import com.cassonade.client.ambience.Birds;

/** The Milky Way on a clear night; the aurora on an odd night over snow. */
public class NightSkyClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			// Only what this test calls
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			// A clear night, looking at the Milky Way's bulge, high to the north
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~ ~ 180 -55");
			context.waitTicks(10);
			Screenshots.take(context, "cassonade-ambience-milky-way");

			// The next night over snow, looking north
			world.getServer().runCommand("execute as @p at @s run fillbiome ~-8 -64 ~-8 ~8 -40 ~8 minecraft:snowy_plains");
			world.getServer().runCommand("time set 42000");
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~ ~ 180 -25");
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> Aurora.strength() >= 1, 20 * 10);
			Screenshots.take(context, "cassonade-ambience-aurora");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
