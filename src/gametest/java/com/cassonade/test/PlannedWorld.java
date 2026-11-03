package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;


/** A new survival world at noon with the quest question answered, for the small tests of a single feature. */
final class PlannedWorld {
	private PlannedWorld() {
	}

	static TestSingleplayerContext open(ClientGameTestContext context) {
		TestSingleplayerContext world = context.worldBuilder().create();
		world.getConnection().waitForChunksRender();
		world.getServer().runCommand("time set noon");
		world.getServer().runCommand("gamemode survival @a");
		world.getServer().runCommand("fill 0 100 0 15 100 15 stone");
		world.getServer().runCommand("fill 0 101 0 15 112 15 air");
		world.getServer().runCommand("tp @a 2.5 101 2.5");
		context.waitTicks(10);
		return world;
	}

	/** Loads the chunks of an area away from the player and waits for them, as the fills refuse unloaded chunks. */
	static void load(ClientGameTestContext context, TestSingleplayerContext world, int minX, int minZ, int maxX, int maxZ) {
		world.getServer().runCommand("forceload add " + minX + " " + minZ + " " + maxX + " " + maxZ);
		for (int i = 0; i < 200 && !world.getServer().computeOnServer(server -> server.overworld().isLoaded(new BlockPos(minX, 0, minZ))
				&& server.overworld().isLoaded(new BlockPos(maxX, 0, maxZ))); i++) {
			context.waitTick();
		}
	}

	static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
