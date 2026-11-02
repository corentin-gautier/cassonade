package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.entity.npc.villager.Villager;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Breath;

/** Breath shows in the cold, outdoors, for players and villagers; not in a temperate land. */
public class BreathClientGameTest implements FabricClientGameTest {
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
			check(!context.computeOnClient(client -> Breath.breathesMist(client.level, client.player)), "not in the plains");
			world.getServer().runCommand("execute as @p at @s run fillbiome ~-8 -64 ~-8 ~8 -40 ~8 minecraft:snowy_plains");
			context.waitFor(client -> Breath.breathesMist(client.level, client.player), 20 * 5);

			world.getServer().runCommand("execute as @p at @s run summon minecraft:villager ~ ~ ~-3 {NoAI:1b,Rotation:[0f,0f]}");
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~ ~ 180 0");
			world.getConnection().waitForClientboundPackets();
			// A few ticks after the villager's breath, its period and offset as in Breath
			context.waitFor(client -> {
				for (var entity : client.level.entitiesForRendering()) {
					if (entity instanceof Villager) {
						return (client.level.getGameTime() + entity.getId() * 13L) % 70 == 6;
					}
				}
				return false;
			}, 20 * 8);
			Screenshots.take(context, "cassonade-breath");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
