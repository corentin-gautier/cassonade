package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.cassonade.client.ambience.Birds;

/** Looking up from under water, in a pool open to the sky: the sun and the moon glow in Snell's window, and the surface around it is darker than the window. */
public class UnderwaterSkyClientGameTest implements FabricClientGameTest {
	// High above the ground, so that the pool's walls are all that lies under the surface
	private static final int SURFACE = 220;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("fill -24 " + (SURFACE - 8) + " -24 24 " + SURFACE + " 24 glass hollow");
			world.getServer().runCommand("fill -23 " + (SURFACE - 7) + " -23 23 " + SURFACE + " 23 water");

			// The sun overhead, then halfway down, at the window's edge
			shot(context, world, 6000, -90, "cassonade-underwater-sky-noon");
			shot(context, world, 6000, -50, "cassonade-underwater-sky-noon-mirror");
			shot(context, world, 3000, -60, "cassonade-underwater-sky-morning");
			shot(context, world, 18000, -90, "cassonade-underwater-sky-midnight");
			shot(context, world, 18000, -50, "cassonade-underwater-sky-midnight-mirror");
		}
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext world, long time, float pitch, String name) {
		world.getServer().runCommand("time set " + time);
		world.getServer().runCommand("tp @a 0 " + (SURFACE - 4) + " 0 -90 " + pitch);
		world.getConnection().waitForClientboundPackets();
		// The eyes get used to the water
		context.waitTicks(80);
		Screenshots.take(context, name);
	}
}
