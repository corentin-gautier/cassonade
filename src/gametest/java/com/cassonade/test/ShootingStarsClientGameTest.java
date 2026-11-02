package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.ShootingStars;

/** Shooting stars only on a clear night, gone in under a second. */
public class ShootingStarsClientGameTest implements FabricClientGameTest {
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
			check(!context.computeOnClient(client -> ShootingStars.canFall(client.level)), "none by day");
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~ ~ 180 -50");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			check(context.computeOnClient(client -> ShootingStars.canFall(client.level)), "on a clear night");
			context.runOnClient(client -> ShootingStars.release(0, 55));
			context.waitTicks(7);
			check(context.computeOnClient(client -> ShootingStars.isFalling()), "it is crossing the sky");
			Screenshots.take(context, "cassonade-shooting-star");
			// By day, so that no other one starts meanwhile
			world.getServer().runCommand("time set 6000");
			context.waitTicks(16);
			check(!context.computeOnClient(client -> ShootingStars.isFalling()), "it is gone");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
