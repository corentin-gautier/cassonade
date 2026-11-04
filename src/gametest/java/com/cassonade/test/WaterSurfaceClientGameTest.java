package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.cassonade.client.ambience.Birds;

/** Still water drawn over several blocks: a wide pool seen from above and skimming, with no grid of repeated blocks. */
public class WaterSurfaceClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule send_command_feedback false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("execute as @p at @s run fill ~-40 ~-3 ~-40 ~40 ~-1 ~40 minecraft:water");
			world.getServer().runCommand("execute as @p at @s run fillbiome ~-40 -64 ~-40 ~40 -40 ~40 minecraft:ocean");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(20);
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~6 ~ 0 90");
			world.getConnection().waitForClientboundPackets();
			world.getConnection().waitForChunksRender();
			context.waitTicks(3);
			Screenshots.take(context, "cassonade-water-surface-above");
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~ ~-36 0 35");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(3);
			Screenshots.take(context, "cassonade-water-surface-across");
		}
	}
}
