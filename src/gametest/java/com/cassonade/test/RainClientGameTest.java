package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.cassonade.client.ambience.Birds;

/** The rain falls in pale grey streaks, the color of water, not vanilla's blue: by day and at night. */
public class RainClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather rain");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~2 ~ 0 10");
			world.getConnection().waitForChunksRender();
			for (String[] shot : new String[][] { { "6000", "cassonade-rain-day" }, { "18000", "cassonade-rain-night" } }) {
				world.getServer().runCommand("time set " + shot[0]);
				// The rain fades in
				context.waitTicks(100);
				context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
				Screenshots.take(context, shot[1]);
			}
		}
	}
}
