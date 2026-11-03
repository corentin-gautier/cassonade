package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;

import com.cassonade.client.ambience.AfterRain;

/** Once the rain stops, the leaves it fell on drip for a while, not those under a roof. */
public class AfterRainClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamerule advance_weather false");
			// Two leaves under a lone one, and one under stone
			world.getServer().runCommand("fill 0 -58 0 0 -56 0 oak_leaves[persistent=true]");
			world.getServer().runCommand("setblock 3 -58 0 oak_leaves[persistent=true]");
			world.getServer().runCommand("setblock 3 -55 0 stone");
			context.waitTicks(20);
			check(context.computeOnClient(client -> AfterRain.wetness(client.level)) == 0, "dry before any rain");
			world.getServer().runCommand("weather rain");
			context.waitFor(client -> client.level.isRaining(), 20 * 20);
			check(context.computeOnClient(client -> AfterRain.wetness(client.level)) == 0, "vanilla's own drops while it rains");
			world.getServer().runCommand("weather clear");
			context.waitFor(client -> !client.level.isRaining(), 20 * 20);
			context.waitTicks(20);
			context.runOnClient(client -> {
				check(AfterRain.wetness(client.level) > 0.5F, "dripping right after the rain");
				check(AfterRain.wasRainedOn(client.level, new BlockPos(0, -58, 0)), "the lowest leaves under leaves drip too");
				check(!AfterRain.wasRainedOn(client.level, new BlockPos(3, -58, 0)), "none under a roof");
			});
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
