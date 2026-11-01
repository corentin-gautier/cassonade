package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

/** A new survival world at noon with the quest question answered, for the small tests of a single feature. */
final class PlannedWorld {
	private PlannedWorld() {
	}

	static TestSingleplayerContext open(ClientGameTestContext context) {
		TestSingleplayerContext world = context.worldBuilder().create();
		world.getConnection().waitForChunksRender();
		context.waitForScreen(QuestOptInScreen.class);
		context.runOnClient(client -> QuestLog.setEnabled(false));
		context.setScreen(() -> null);
		world.getServer().runCommand("time set noon");
		world.getServer().runCommand("gamemode survival @a");
		world.getServer().runCommand("fill 0 100 0 15 100 15 stone");
		world.getServer().runCommand("fill 0 101 0 15 112 15 air");
		world.getServer().runCommand("tp @a 2.5 101 2.5");
		context.waitTicks(10);
		return world;
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
