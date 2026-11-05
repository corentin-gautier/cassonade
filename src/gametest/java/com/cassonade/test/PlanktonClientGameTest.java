package com.cassonade.test;

import java.util.Arrays;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Foam;
import com.cassonade.client.ambience.Plankton;
import com.cassonade.client.ambience.Waves;

/** Banks of glowing plankton: a share of the sea on some nights, sparkling calm, and the foam lighting up where a wave breaks in one. */
public class PlanktonClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		// Banks cover part of the sea on a night of banks, and none on the others
		int[] covered = new int[8];
		for (int night = 0; night < covered.length; night++) {
			for (int x = -200; x < 200; x += 4) {
				for (int z = -200; z < 200; z += 4) {
					covered[night] += Plankton.inBank(x, z, night) ? 1 : 0;
				}
			}
		}
		int cells = 100 * 100;
		check(Arrays.stream(covered).allMatch(count -> count > cells / 50 && count < cells / 2), "banks over a share of the sea: " + Arrays.toString(covered));
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				Birds.setSpawning(false);
				Waves.setSurging(false);
			});
			world.getServer().runCommand("gamerule send_command_feedback false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("execute as @p at @s run fill ~-12 ~-1 ~-16 ~7 ~-1 ~16 minecraft:sand");
			world.getServer().runCommand("execute as @p at @s run fill ~8 ~-3 ~-16 ~30 ~-1 ~16 minecraft:water");
			world.getServer().runCommand("execute as @p at @s run fillbiome ~6 -64 ~-16 ~30 -40 ~16 minecraft:cold_ocean");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(20);
			context.runOnClient(client -> Plankton.setEverywhere(true));
			// Hovering over the sand, looking out at the sea
			world.getServer().runCommand("execute as @p at @s run tp @a ~2 ~5 ~ -90 35");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(20);
			check(context.computeOnClient(client -> Plankton.sparks()) == 0, "no plankton in a cold sea");

			world.getServer().runCommand("execute as @p at @s run fillbiome ~4 -64 ~-16 ~28 -40 ~16 minecraft:warm_ocean");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(30);
			check(context.computeOnClient(client -> Plankton.sparks()) > 0, "a warm sea's bank sparkling, calm");
			Screenshots.take(context, "plankton-calm");
			context.runOnClient(client -> Foam.surge(client, client.level));
			check(context.computeOnClient(client -> Foam.glowing()) > 20, "the foam glowing where the wave breaks");
			context.waitTicks(12);
			Screenshots.take(context, "plankton-wave");

			world.getServer().runCommand("time set 6000");
			context.waitTicks(20);
			check(context.computeOnClient(client -> Plankton.sparks()) == 0, "no glow by day");
		} finally {
			context.runOnClient(client -> {
				Plankton.setEverywhere(false);
				Waves.setSurging(true);
			});
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
