package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Murmuration;

/** Starlings only in the last hour of a clear day; a cloud that comes, stays and goes on, skipped through rather than waited for. */
public class MurmurationClientGameTest implements FabricClientGameTest {
	private static final int DURATION = 1600;

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
			context.waitTicks(5);
			check(!context.computeOnClient(Murmuration::canGather), "no starlings at noon");
			world.getServer().runCommand("time set 18000");
			context.waitTicks(5);
			check(!context.computeOnClient(Murmuration::canGather), "no starlings at night");
			world.getServer().runCommand("time set 11300");
			context.waitTicks(5);
			check(context.computeOnClient(Murmuration::canGather), "starlings can come as a clear day ends");

			// Called by the development command, as in runClient
			context.runOnClient(client -> client.player.connection.sendCommand("ambience murmuration"));
			context.waitTicks(1);
			check(context.computeOnClient(client -> Murmuration.isGathered()), "the development command calls a cloud");
			Vec3 start = context.computeOnClient(client -> Murmuration.position());
			context.runOnClient(client -> Murmuration.advance(DURATION / 2));
			Vec3 middle = context.computeOnClient(client -> Murmuration.position());
			check(middle.distanceTo(start) > 200, "the cloud comes in");
			aimAt(context, world, middle);
			Screenshots.take(context, "cassonade-starlings-1");
			context.runOnClient(client -> Murmuration.advance(100));
			Vec3 later = context.computeOnClient(client -> Murmuration.position());
			check(later.distanceTo(middle) < 40, "the cloud stays a while");
			context.waitTicks(1);
			Screenshots.take(context, "cassonade-starlings-2");
			context.runOnClient(client -> Murmuration.advance(DURATION));
			context.waitTicks(1);
			check(!context.computeOnClient(client -> Murmuration.isGathered()), "the cloud is gone");
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
