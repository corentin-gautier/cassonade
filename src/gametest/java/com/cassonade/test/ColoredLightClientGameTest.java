package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * A closed stone room deep down, with six bays along its far wall, lit by a torch, a soul lantern, a
 * redstone torch, a copper torch, a sea lantern and amethyst: each tints the stone around it with its own color.
 * The mod's own lights too: a waystone's crystal, and the sculk torch in hand.
 */
public class ColoredLightClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ -40 ~ 180 20");
			world.getServer().runCommand("execute as @a at @s run fill ~-12 ~-1 ~-12 ~12 ~6 ~12 stone hollow");
			// Six bays along the far wall, split by walls, each with its light
			String[] lights = { "torch", "soul_lantern", "redstone_torch", "copper_torch", "sea_lantern", "amethyst_cluster" };
			for (int i = 0; i <= lights.length; i++) {
				int wall = i * 4 - 12;
				world.getServer().runCommand("execute as @a at @s run fill ~" + wall + " ~ ~-11 ~" + wall + " ~5 ~-6 stone");
			}
			for (int i = 0; i < lights.length; i++) {
				world.getServer().runCommand("execute as @a at @s run setblock ~" + (i * 4 - 10) + " ~ ~-10 " + lights[i]);
			}
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~2 ~10 180 15");
			context.runOnClient(client -> client.gui.hud.toggle());
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "colored-light-room");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~-12 180 50");
			context.waitTicks(5);
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "colored-light-floor");

			// The mod's own lights: a waystone's crystal, and a sculk torch carried in a dark corner
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("execute as @a at @s run setblock ~4 ~-2 ~4 cassonade:waystone[half=lower]");
			world.getServer().runCommand("execute as @a at @s run setblock ~4 ~-1 ~4 cassonade:waystone[half=upper]");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:sculk_torch");
			world.getServer().runCommand("execute as @a at @s run tp @s ~-6 ~-2 ~10 -120 20");
			context.waitTicks(30);
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "colored-light-mod");

			// Outdoors the sun washes the colors out by day; they come back at night, without the mesh built again
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ 0 ~ 180 15");
			world.getServer().runCommand("execute as @a at @s run fill ~-6 ~-1 ~-8 ~6 ~4 ~-8 stone");
			world.getServer().runCommand("execute as @a at @s run fill ~-6 ~ ~-7 ~6 ~3 ~-7 glow_lichen[north=true]");
			world.getServer().runCommand("execute as @a at @s run setblock ~-3 ~ ~-5 soul_lantern");
			world.getServer().runCommand("execute as @a at @s run setblock ~3 ~ ~-5 redstone_torch");
			world.getServer().runCommand("time set 6000");
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "colored-light-day");
			world.getServer().runCommand("time set 18000");
			context.waitTicks(20);
			Screenshots.take(context, "colored-light-night");
		}
	}
}
