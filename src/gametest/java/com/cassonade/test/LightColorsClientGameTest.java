package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Every light with a color of its own, side by side to compare how much each one shows: the closed stone
 * room of ColoredLight, built again for each group of five lights, a plain torch in the first bay as the
 * warm light they stand against. One screenshot per group, named after its lights.
 */
public class LightColorsClientGameTest implements FabricClientGameTest {
	/** Each light: the block placed and its height above the floor, -1 sunk into the floor, 5 hung from the ceiling. */
	private record Light(String block, int height) {
		Light(String block) {
			this(block, 0);
		}

		String name() {
			String name = block.replace("cassonade:", "");
			int state = name.indexOf('[');
			return state < 0 ? name : name.substring(0, state);
		}
	}

	private static final Light[][] GROUPS = {
			{ new Light("soul_torch"), new Light("soul_lantern"), new Light("soul_campfire"), new Light("soul_fire"), new Light("redstone_torch") },
			{ new Light("redstone_ore[lit=true]"), new Light("copper_torch"), new Light("copper_lantern"), new Light("copper_bulb[lit=true]"), new Light("amethyst_cluster") },
			{ new Light("crying_obsidian"), new Light("respawn_anchor[charges=4]"), new Light("sea_lantern"), new Light("conduit[waterlogged=false]"), new Light("beacon") },
			{ new Light("lava", -1), new Light("magma_block", -1), new Light("shroomlight"), new Light("glowstone"), new Light("glow_lichen[down=true]") },
			{ new Light("ochre_froglight"), new Light("verdant_froglight"), new Light("pearlescent_froglight"), new Light("cave_vines[berries=true]", 5), new Light("sculk_catalyst") },
			{ new Light("sculk_sensor"), new Light("calibrated_sculk_sensor"), new Light("cassonade:glowcap"), new Light("cassonade:waystone[half=lower]"), new Light("lantern") },
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ -40 ~ 180 15");
			context.runOnClient(client -> client.gui.hud.toggle());
			for (Light[] group : GROUPS) {
				// The room again, emptied of the last group's lights, then six bays along its far wall
				world.getServer().runCommand("execute as @a at @s run fill ~-12 ~-3 ~-22 ~12 ~4 ~2 stone hollow");
				for (int i = 0; i <= 6; i++) {
					int wall = i * 4 - 12;
					world.getServer().runCommand("execute as @a at @s run fill ~" + wall + " ~-2 ~-21 ~" + wall + " ~3 ~-16 stone");
				}
				StringBuilder name = new StringBuilder("light-colors");
				for (int i = 0; i < 6; i++) {
					Light light = i == 0 ? new Light("torch") : group[i - 1];
					int x = i * 4 - 10;
					if (light.block().startsWith("soul_fire")) {
						world.getServer().runCommand("execute as @a at @s run setblock ~" + x + " ~-3 ~-20 soul_soil");
					}
					world.getServer().runCommand("execute as @a at @s run setblock ~" + x + " ~" + (light.height() - 2) + " ~-20 " + light.block());
					if (light.block().startsWith("cassonade:waystone")) {
						world.getServer().runCommand("execute as @a at @s run setblock ~" + x + " ~-1 ~-20 cassonade:waystone[half=upper]");
					}
					if (i > 0) {
						name.append('-').append(light.name());
					}
				}
				context.waitTicks(20);
				world.getConnection().waitForChunksRender();
				Screenshots.take(context, name.toString());
			}
			context.runOnClient(client -> client.gui.hud.toggle());
		}
	}
}
