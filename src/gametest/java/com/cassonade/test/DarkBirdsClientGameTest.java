package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Birds;

/**
 * A flock of crows, seen up close.
 */
public class DarkBirdsClientGameTest implements FabricClientGameTest {
	/** Half of a flock's 400 blocks at 0.45 blocks a tick. */
	private static final int TICKS_TO_MIDDLE = 444;

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
			world.getServer().runCommand("weather clear");

			// By day, from up close
			world.getServer().runCommand("time set 6000");
			context.runOnClient(client -> Birds.releaseCrows(client, 50));
			context.runOnClient(client -> Birds.advance(TICKS_TO_MIDDLE));
			Vec3 crows = context.computeOnClient(client -> Birds.position());
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("tp @a " + crows.x + " " + (crows.y - 7) + " " + crows.z);
			world.getConnection().waitForClientboundPackets();
			aimAt(context, world, context.computeOnClient(client -> Birds.position()));
			Screenshots.take(context, "cassonade-crows");
		}
	}

	private static void aimAt(ClientGameTestContext context, TestSingleplayerContext world, Vec3 target) {
		Vec3 to = target.subtract(context.computeOnClient(client -> client.player.getEyePosition()));
		double yaw = Math.toDegrees(Math.atan2(-to.x, to.z));
		double pitch = -Math.toDegrees(Math.atan2(to.y, Math.hypot(to.x, to.z)));
		world.getServer().runCommand("execute as @p at @s run tp @a ~ ~ ~ " + yaw + " " + pitch);
		world.getConnection().waitForClientboundPackets();
		context.waitTicks(2);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
