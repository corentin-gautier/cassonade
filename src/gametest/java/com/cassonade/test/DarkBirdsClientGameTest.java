package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Murmuration;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

/**
 * A flock of crows, seen up close; starlings only in the last hour of a clear day, a cloud that comes, stays and goes on.
 */
public class DarkBirdsClientGameTest implements FabricClientGameTest {
	/** Half of a flock's 400 blocks at 0.45 blocks a tick. */
	private static final int TICKS_TO_MIDDLE = 444;
	private static final int CLOUD_DURATION = 1600;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
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

			context.runOnClient(client -> Murmuration.release(client));
			Vec3 start = context.computeOnClient(client -> Murmuration.position());
			context.waitTicks(CLOUD_DURATION / 2);
			Vec3 middle = context.computeOnClient(client -> Murmuration.position());
			check(middle.distanceTo(start) > 200, "the cloud comes in");
			aimAt(context, world, middle);
			Screenshots.take(context, "cassonade-starlings-1");
			context.waitTicks(100);
			Vec3 later = context.computeOnClient(client -> Murmuration.position());
			check(later.distanceTo(middle) < 40, "the cloud stays a while");
			Screenshots.take(context, "cassonade-starlings-2");
			context.waitTicks(CLOUD_DURATION / 2);
			check(!context.computeOnClient(client -> Murmuration.isGathered()), "the cloud is gone");

			// Crows, by day, from up close
			world.getServer().runCommand("time set 6000");
			context.runOnClient(client -> Birds.releaseCrows(client, 50));
			context.waitTicks(TICKS_TO_MIDDLE);
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
