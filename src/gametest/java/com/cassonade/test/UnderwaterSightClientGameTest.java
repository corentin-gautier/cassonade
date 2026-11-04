package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.resources.Identifier;

import com.cassonade.Cassonade;
import com.cassonade.client.ambience.Birds;

/** Under water the view is short and blurred: pillars 4, 8, 16 and 32 blocks away, the nearest blurred, the farthest lost. Above it, no blur. */
public class UnderwaterSightClientGameTest implements FabricClientGameTest {
	private static final int SURFACE = 120;
	private static final Identifier BLUR = Cassonade.id("underwater");

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
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("forceload add -8 -8 48 8");
			world.getServer().runCommand("fill -4 " + (SURFACE - 12) + " -8 44 " + SURFACE + " 8 glass hollow");
			world.getServer().runCommand("fill -3 " + (SURFACE - 11) + " -7 43 " + SURFACE + " 7 water");
			for (int distance : new int[] { 4, 8, 16, 32 }) {
				world.getServer().runCommand("fill " + distance + " " + (SURFACE - 11) + " -1 " + distance + " " + (SURFACE - 2) + " -1 yellow_wool");
				world.getServer().runCommand("fill " + distance + " " + (SURFACE - 11) + " 1 " + distance + " " + (SURFACE - 2) + " 1 red_wool");
			}

			world.getServer().runCommand("tp @a 0.5 " + (SURFACE - 6) + " 0.5 -90 0");
			world.getConnection().waitForChunksRender();
			// The eyes get used to the water
			context.waitTicks(80);
			check(context.computeOnClient(client -> client.gameRenderer.getRequestedPostEffects().contains(BLUR)), "the view blurred under water");
			// Asked for and drawn: a shader that fails to compile is dropped by the game with only a warning
			check(context.computeOnClient(client -> client.gameRenderer.getAppliedPostEffects().contains(BLUR)), "the blur drawn");
			Screenshots.take(context, "cassonade-underwater-sight");

			world.getServer().runCommand("tp @a 0.5 " + (SURFACE + 2) + " 0.5 -90 20");
			context.waitTicks(5);
			check(context.computeOnClient(client -> !client.gameRenderer.getRequestedPostEffects().contains(BLUR)), "the view sharp above the water");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
