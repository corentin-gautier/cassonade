package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Butterflies;

/** Butterflies around flowers by day; bees, adult and baby, drawn smaller. */
public class InsectsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			// Only what this test calls
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 6000");

			// Flowers around, in a temperate biome
			world.getServer().runCommand("execute as @p at @s run fillbiome ~-8 -64 ~-8 ~8 -40 ~8 minecraft:plains");
			world.getServer().runCommand("execute as @p at @s run fill ~-6 ~ ~-6 ~6 ~ ~6 minecraft:poppy replace air");
			context.waitFor(client -> Butterflies.count() > 0, 20 * 30);
			// Once fully grown in, from up close
			context.waitTicks(40);
			world.getServer().runCommand("gamemode spectator @a");
			Vec3 first = context.computeOnClient(client -> Butterflies.first());
			world.getServer().runCommand("tp @a " + first.x + " " + (first.y - 0.6) + " " + (first.z - 1.4) + " 0 20");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(1);
			Screenshots.take(context, "cassonade-ambience-butterfly");

			// Bees, adult and baby, drawn smaller; beside an oak log for scale
			world.getServer().runCommand("setblock 20 -59 24 minecraft:oak_log");
			world.getServer().runCommand("summon minecraft:bee 19 -59 24 {NoAI:1b}");
			world.getServer().runCommand("summon minecraft:bee 21.5 -59 24 {NoAI:1b,Age:-24000}");
			world.getServer().runCommand("tp @a 20.5 -58 21.5 0 10");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			Screenshots.take(context, "cassonade-ambience-bees");
		}
	}
}
