package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.JumpingFish;

/** Fish leap out of deep water at dawn and dusk, never out of dry land. */
public class JumpingFishClientGameTest implements FabricClientGameTest {
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
			check(!context.computeOnClient(client -> JumpingFish.canLeap(client.level)), "none at noon");
			world.getServer().runCommand("time set 12000");
			context.waitTicks(2);
			check(context.computeOnClient(client -> JumpingFish.canLeap(client.level)), "at dusk");
			check(!context.computeOnClient(client -> JumpingFish.release(client, client.level)), "not out of dry land");

			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("execute as @p at @s run fill ~-24 ~-3 ~-24 ~24 ~-1 ~24 minecraft:water");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			check(context.computeOnClient(client -> JumpingFish.release(client, client.level)), "out of a pond");
			// Held in the air by day, to be seen from the side at each moment of the leap
			context.runOnClient(client -> JumpingFish.hold(0));
			world.getServer().runCommand("time set 6000");
			Vec3 from = context.computeOnClient(client -> JumpingFish.position());
			context.runOnClient(client -> JumpingFish.hold(7));
			Vec3 top = context.computeOnClient(client -> JumpingFish.position());
			check(top.y > from.y + 0.5, "it rises out of the water");
			Vec3 along = top.subtract(from).multiply(1, 0, 1).normalize();
			Vec3 eye = top.add(along.z * 2.5, 0, -along.x * 2.5);
			double yaw = Math.toDegrees(Math.atan2(eye.x - top.x, top.z - eye.z));
			world.getServer().runCommand("tp @a " + eye.x + " " + (eye.y - 1.62) + " " + eye.z + " " + yaw + " 0");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(3);
			for (int age : new int[] {3, 7, 11}) {
				context.runOnClient(client -> JumpingFish.hold(age));
				context.waitTicks(2);
				Screenshots.take(context, "cassonade-jumping-fish-" + age);
			}
			context.runOnClient(client -> JumpingFish.hold(-1));
			context.waitFor(client -> !JumpingFish.isLeaping(), 20);
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
