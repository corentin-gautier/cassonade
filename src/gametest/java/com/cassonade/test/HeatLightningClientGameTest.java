package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;


import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.HeatLightning;

/** Silent glows on the horizon, on some warm evenings only. */
public class HeatLightningClientGameTest implements FabricClientGameTest {
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

			world.getServer().runCommand("time set 6000");
			context.waitTicks(2);
			check(!context.computeOnClient(client -> HeatLightning.isEvening(client.level)), "none by day");
			check(context.computeOnClient(client -> HeatLightning.isWarm(client.level, client.player.blockPosition())), "plains are warm enough");
			// Not every evening: the next that has it, within a few days
			int day = 0;
			world.getServer().runCommand("time set " + (24000 * day + 13500));
			context.waitTicks(2);
			while (!context.computeOnClient(client -> HeatLightning.isEvening(client.level))) {
				check(++day < 40, "some evenings have it");
				world.getServer().runCommand("time set " + (24000 * day + 13500));
				context.waitTicks(2);
			}
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~ ~ 180 -5");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-heat-lightning-before");
			context.runOnClient(client -> HeatLightning.flash(0));
			context.waitTicks(1);
			check(context.computeOnClient(client -> HeatLightning.isFlashing()), "it flickers");
			Screenshots.take(context, "cassonade-heat-lightning");
			// By day, so that no other one starts meanwhile
			world.getServer().runCommand("time set 6000");
			context.waitTicks(15);
			check(!context.computeOnClient(client -> HeatLightning.isFlashing()), "it is over");
			world.getServer().runCommand("execute as @p at @s run fillbiome ~-8 -64 ~-8 ~8 -40 ~8 minecraft:snowy_plains");
			context.waitFor(client -> !HeatLightning.isWarm(client.level, client.player.blockPosition()), 20 * 5);
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
