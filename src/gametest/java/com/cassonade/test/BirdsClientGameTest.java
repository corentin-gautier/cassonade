package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.AirSounds;
import com.cassonade.client.ambience.Birds;

/**
 * Flocks only in clear daylight; one released crosses the sky, seen as it passes closest, and leaves it once across.
 */
public class BirdsClientGameTest implements FabricClientGameTest {
	/** Half of its 400 blocks at 0.45 blocks a tick. */
	private static final int TICKS_TO_MIDDLE = 444;
	private static final int TICKS_TO_END = 900;
	private static final int RELEASES = 40;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			// Only the flocks this test releases
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");

			world.getServer().runCommand("time set 18000");
			context.waitTicks(5);
			check(!context.computeOnClient(Birds::canFly), "no flock at night");
			world.getServer().runCommand("time set 6000");
			context.waitTicks(5);
			check(context.computeOnClient(Birds::canFly), "a flock can come in clear daylight");
			check(context.computeOnClient(client -> client.getSoundManager().getSoundEvent(Birds.CALL).getWeight() > 0), "the flock's call has its sounds");
			context.runOnClient(client -> Birds.release(client, 50));
			Vec3 start = context.computeOnClient(client -> Birds.position());
			context.runOnClient(client -> Birds.advance(TICKS_TO_MIDDLE));
			Vec3 middle = context.computeOnClient(client -> Birds.position());
			check(middle.distanceTo(start) > 150, "the flock crosses the sky");
			aimAtFlock(context, world);
			Screenshots.take(context, "cassonade-birds-1");
			// From up close, as through a spyglass
			world.getServer().runCommand("gamemode spectator @a");
			Vec3 flock = context.computeOnClient(client -> Birds.position());
			world.getServer().runCommand("tp @a " + flock.x + " " + (flock.y - 7) + " " + flock.z);
			world.getConnection().waitForClientboundPackets();
			aimAtFlock(context, world);
			Screenshots.take(context, "cassonade-birds-2");

			context.runOnClient(client -> Birds.advance(TICKS_TO_END - TICKS_TO_MIDDLE));
			context.waitTicks(1);
			check(!context.computeOnClient(client -> Birds.isFlying()), "the flock is gone once across");

			// Inland, no gulls; with a beach near, some among the others
			check(context.computeOnClient(client -> releases(client)) == 0, "no gulls inland");
			world.getServer().runCommand("execute as @p at @s run fillbiome ~24 ~-8 ~-8 ~40 ~8 ~8 minecraft:beach");
			context.waitFor(client -> AirSounds.nearCoast(client.level, client.player.blockPosition()), 20 * 5);
			long gulls = context.computeOnClient(client -> releases(client));
			check(gulls > 0 && gulls < RELEASES, "gulls among the others near a beach (" + gulls + ")");

			// Gulls, from up close
			context.runOnClient(client -> Birds.setSpawning(false));
			context.runOnClient(client -> Birds.releaseGulls(client, 50));
			context.runOnClient(client -> Birds.advance(TICKS_TO_MIDDLE));
			Vec3 close = context.computeOnClient(client -> Birds.position());
			world.getServer().runCommand("tp @a " + close.x + " " + (close.y - 7) + " " + close.z);
			world.getConnection().waitForClientboundPackets();
			aimAtFlock(context, world);
			Screenshots.take(context, "cassonade-birds-gulls");

			// Several at once
			context.runOnClient(client -> Birds.setSpawning(true));
			context.runOnClient(client -> Birds.callNext());
			context.waitTicks(1);
			context.runOnClient(client -> Birds.callNext());
			context.waitFor(client -> Birds.flockCount() >= 2, 20);

			// No flock in the rain, once it has set in on the client
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("weather rain");
			context.waitFor(client -> !Birds.canFly(client), 20 * 10);
		}
	}

	/** How many of a good many flocks, released as the game does, are gulls; the sky left empty. */
	private static long releases(Minecraft client) {
		for (int i = 0; i < RELEASES; i++) {
			Birds.release(client);
		}
		long gulls = Birds.gullFlocks();
		Birds.setSpawning(false);
		return gulls;
	}

	private static void aimAtFlock(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3 eye = context.computeOnClient(client -> client.player.getEyePosition());
		Vec3 to = context.computeOnClient(client -> Birds.position()).subtract(eye);
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
