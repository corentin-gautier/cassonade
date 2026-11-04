package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** The raised shield and the flames of a burning player, lowered on the screen: screenshots to look at. */
public class ScreenOverlaysClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("item replace entity @a weapon.offhand with shield");
			context.waitTicks(10);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(10);
			Screenshots.take(context, "screen-overlays-shield");
			context.getInput().releaseKey(options -> options.keyUse);
			world.getServer().runCommand("effect give @a fire_resistance 60 0 true");
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setRemainingFireTicks(400));
			context.waitTicks(10);
			Screenshots.take(context, "screen-overlays-fire");
		}
	}
}
