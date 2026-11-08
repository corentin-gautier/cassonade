package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Water darkens at night everywhere, falls and slopes as much as a still pool, while glass beside it keeps its color. */
public class NightWaterClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("kill @e[type=!player]");
			// A pool, a pillar with a spring on top falling into it, and a block of glass for comparison
			world.getServer().runCommand("fill -10 95 -2 10 110 20 air");
			world.getServer().runCommand("fill -10 94 -2 10 94 20 stone");
			world.getServer().runCommand("fill -6 95 4 6 95 14 water");
			world.getServer().runCommand("fill -1 95 8 1 98 10 stone");
			world.getServer().runCommand("setblock 0 99 9 water");
			world.getServer().runCommand("setblock 4 96 6 light_blue_stained_glass");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("tp @a 0.5 102 1.5 0 30");
			context.runOnClient(client -> client.gui.hud.toggle());

			shot(context, world, 6000, "night-water-day");
			shot(context, world, 18000, "night-water-night");
		}
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext world, int time, String name) {
		world.getServer().runCommand("time set " + time);
		context.waitTicks(40);
		world.getConnection().waitForChunksRender();
		Screenshots.take(context, name);
	}
}
