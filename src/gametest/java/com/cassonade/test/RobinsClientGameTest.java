package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Robins;

/**
 * A robin perched on a tree top stays while the player keeps away, flies off as they come near, and is gone once far off;
 * another lands there before the eyes.
 */
public class RobinsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				Birds.setSpawning(false);
				Robins.setSpawning(false);
			});
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 6000");
			// A small tree alone in a clearing, one leaf block on its top for the robin
			BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
			BlockPos crown = feet.offset(0, 1, 8);
			BlockPos leaves = crown.above();
			world.getServer().runCommand(fill(feet.offset(-12, 0, -12), feet.offset(12, 24, 20), "air"));
			// Clear beyond the tree too, where the next robin comes in from
			world.getServer().runCommand(fill(feet.offset(-20, 0, 21), feet.offset(20, 24, 36), "air"));
			world.getServer().runCommand(fill(feet.offset(-12, -1, -12), feet.offset(12, -1, 20), "grass_block"));
			world.getServer().runCommand(fill(feet.offset(0, 0, 8), feet.offset(0, 0, 8), "oak_log"));
			world.getServer().runCommand(fill(crown.offset(-1, 0, -1), crown.offset(1, 0, 1), "oak_leaves[persistent=true]"));
			world.getServer().runCommand(fill(leaves, leaves, "oak_leaves[persistent=true]"));
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(5);
			context.runOnClient(client -> Robins.perch(client, leaves));

			// From up close, unseen
			world.getServer().runCommand("gamemode spectator @a");
			look(context, world, new Vec3(leaves.getX() + 1.9, leaves.getY() + 1.3, leaves.getZ() + 2.4));
			Screenshots.take(context, "cassonade-robins-perched");
			look(context, world, new Vec3(leaves.getX() - 1.4, leaves.getY() + 1.5, leaves.getZ() - 1.2));
			Screenshots.take(context, "cassonade-robins-perched-back");

			// A player at a distance leaves it be; one coming near sends it off
			world.getServer().runCommand("gamemode creative @a");
			look(context, world, new Vec3(leaves.getX() + 0.5, feet.getY() + 1.62, leaves.getZ() - 10));
			context.waitTicks(20);
			check(context.computeOnClient(client -> Robins.count() == 1 && Robins.flying() == 0), "it stays while the player keeps away");
			look(context, world, new Vec3(leaves.getX() + 0.5, feet.getY() + 1.62, leaves.getZ() - 4));
			context.waitFor(client -> Robins.flying() == 1, 10);
			context.waitTicks(10);
			look(context, world, context.computeOnClient(client -> client.player.getEyePosition()));
			Screenshots.take(context, "cassonade-robins-flying");
			context.waitFor(client -> Robins.count() == 0, 20 * 6);

			// Another comes in from beyond the tree, before the eyes, and settles on it
			// Seen from afar, so it does not turn back on the first tick
			world.getServer().runCommand("gamemode spectator @a");
			look(context, world, new Vec3(leaves.getX() + 0.5, leaves.getY() + 1.5, leaves.getZ() - 8), Vec3.atBottomCenterOf(leaves.above()));
			check(context.computeOnClient(client -> Robins.land(client, leaves)), "a clear way in");
			context.waitTicks(45);
			Screenshots.take(context, "cassonade-robins-landing");
			context.waitFor(client -> Robins.landing() == 0, 40);
			check(context.computeOnClient(client -> Robins.count() == 1 && Robins.flying() == 0), "it stays on its tree once landed");
		}
	}

	private static String fill(BlockPos from, BlockPos to, String block) {
		return "fill " + from.getX() + " " + from.getY() + " " + from.getZ() + " " + to.getX() + " " + to.getY() + " " + to.getZ() + " " + block;
	}

	/** The eyes here, turned towards the robin. */
	private static void look(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye) {
		look(context, world, eye, context.computeOnClient(client -> Robins.first()));
	}

	/** The eyes here, turned towards where a robin stands. */
	private static void look(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 robin) {
		Vec3 to = robin.add(0, 0.15, 0).subtract(eye);
		double yaw = Math.toDegrees(Math.atan2(-to.x, to.z));
		double pitch = -Math.toDegrees(Math.atan2(to.y, Math.hypot(to.x, to.z)));
		world.getServer().runCommand("tp @a " + eye.x + " " + (eye.y - 1.62) + " " + eye.z + " " + yaw + " " + pitch);
		world.getConnection().waitForClientboundPackets();
		context.waitTicks(3);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
