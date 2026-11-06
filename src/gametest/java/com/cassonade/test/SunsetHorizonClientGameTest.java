package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.cassonade.client.ambience.Birds;

/** The sun setting into a flat sea up to the world's edge, zoomed in as through a spyglass: nothing but sun and sea where they meet. */
public class SunsetHorizonClientGameTest implements FabricClientGameTest {
	/** Past the render distance (6 chunks, 96 blocks), where land would still be drawn, and inside the chunks loaded. */
	private static final int REACH = 104;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.runOnClient(client -> {
				Birds.setSpawning(false);
				client.options.renderDistance().set(6);
				client.options.broadcastOptions();
				client.options.fov().set(30);
			});
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamemode spectator @a");
			int sea = world.getServer().computeOnServer(server -> server.overworld().getSeaLevel());
			// A fill fails whole if it reaches a chunk not loaded yet
			world.getServer().runCommand("tp @a 0 " + (sea + 1) + " 0 90 -1");
			world.getConnection().waitForChunksRender();
			// A sea one block deep at sea level, the ground above it cleared, out past the render distance
			for (int x : new int[] { -REACH, 0 }) {
				for (int z : new int[] { -REACH, 0 }) {
					String from = x + " %d " + z, to = (x + REACH) + " %d " + (z + REACH);
					world.getServer().runCommand("fill " + from.formatted(sea - 1) + " " + to.formatted(sea - 1) + " water");
					// In two, under the game's limit of blocks for one fill
					world.getServer().runCommand("fill " + from.formatted(sea) + " " + to.formatted(sea + 1) + " air");
					world.getServer().runCommand("fill " + from.formatted(sea + 2) + " " + to.formatted(sea + 3) + " air");
				}
			}
			world.getServer().runCommand("time set 12900");
			world.getConnection().waitForChunksRender();
			context.waitTicks(40);
			Screenshots.take(context, "cassonade-sunset-horizon");
			// The sea at midnight, seen from its surface
			context.runOnClient(client -> client.options.fov().set(70));
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("tp @a 0 " + (sea + 1) + " 0 90 10");
			context.waitTicks(20);
			Screenshots.take(context, "cassonade-sunset-horizon-night");
		}
	}
}
