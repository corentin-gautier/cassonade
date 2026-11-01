package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.level.Level;

import com.cassonade.client.map.WorldMap;

public class NetherCoordinatesClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			context.runOnClient(client -> check(!WorldMap.visited(Level.NETHER), "a new world: no Nether yet"));
			world.getServer().runCommand("execute in minecraft:the_nether run tp @a 8 100 16");
			context.waitTicks(60);
			world.getServer().runCommand("execute in minecraft:overworld run tp @a 2.5 101 2.5");
			context.waitTicks(20);
			context.runOnClient(client -> check(WorldMap.visited(Level.NETHER), "the Nether counts once visited, saved or not"));
		}
	}
}
