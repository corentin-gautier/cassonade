package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Motes;

/** The air of a savanna; dust falling in a cave, none in a hut on the surface. */
public class MotesClientGameTest implements FabricClientGameTest {
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

			// Savanna, too hot for the temperate pollen, has its own air
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("execute as @p at @s run fillbiome ~-16 ~-3 ~-16 ~16 ~8 ~16 minecraft:savanna");
			context.waitFor(client -> client.level.getBiome(client.player.blockPosition()).is(Identifier.withDefaultNamespace("savanna")), 20 * 5);
			context.waitTicks(100);
			Screenshots.take(context, "cassonade-ambience-savanna");

			// Dust falling from the ceiling of a lit cave, with rock above it; none in a hut on the surface
			world.getServer().runCommand("fill 40 -60 40 50 -44 50 minecraft:stone");
			world.getServer().runCommand("fill 41 -59 41 49 -55 49 minecraft:air");
			world.getServer().runCommand("fill 60 -60 60 66 -56 66 minecraft:stone hollow");
			world.getServer().runCommand("setblock 45 -55 45 minecraft:sea_lantern");
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("tp @a 45.5 -59 41.5 0 -5");
			world.getConnection().waitForClientboundPackets();
			world.getConnection().waitForChunksRender();
			context.waitTicks(100);
			check(context.computeOnClient(client -> Motes.inCave(client.level, new BlockPos(45, -58, 45))), "under rock is a cave");
			check(!context.computeOnClient(client -> Motes.inCave(client.level, new BlockPos(63, -58, 63))), "a hut is no cave");
			check(context.computeOnClient(client -> Motes.underCeiling(client.level, new BlockPos(44, -55, 45))), "dust starts under the rock");
			check(!context.computeOnClient(client -> Motes.underCeiling(client.level, new BlockPos(44, -58, 45))), "not in mid-air");
			Screenshots.take(context, "cassonade-ambience-dust");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
