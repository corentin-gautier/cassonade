package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Robins;
import com.cassonade.client.ambience.Squirrels;

/**
 * A squirrel comes down a trunk out of the leaves and stops on the bark, another goes down to the foot of the tree and sits
 * up there; it stays while the player keeps away, and runs back up into the leaves as they come near.
 */
public class SquirrelsClientGameTest implements FabricClientGameTest {
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
				Squirrels.setSpawning(false);
			});
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 6000");
			// A small tree alone in a clearing, its trunk bare for three blocks under its leaves
			BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
			BlockPos trunk = feet.offset(0, 0, 8);
			world.getServer().runCommand(fill(feet.offset(-12, 0, -12), feet.offset(12, 24, 20), "air"));
			world.getServer().runCommand(fill(feet.offset(-12, -1, -12), feet.offset(12, -1, 20), "grass_block"));
			world.getServer().runCommand(fill(trunk.offset(-2, 3, -2), trunk.offset(2, 4, 2), "oak_leaves[persistent=true]"));
			world.getServer().runCommand(fill(trunk.offset(-1, 5, -1), trunk.offset(1, 5, 1), "oak_leaves[persistent=true]"));
			world.getServer().runCommand(fill(trunk, trunk.offset(0, 4, 0), "oak_log"));
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(5);

			// Down the bark, seen from up close, unseen
			world.getServer().runCommand("gamemode spectator @a");
			check(context.computeOnClient(client -> Squirrels.comeDown(client, trunk, false)), "a bare trunk under leaves");
			context.waitFor(client -> Squirrels.settled() == 1, 20 * 15);
			look(context, world, trunk, 1.6);
			Screenshots.take(context, "cassonade-squirrels-bark");

			// Down to the foot of the tree, sitting up there
			context.runOnClient(client -> Squirrels.setSpawning(false));
			check(context.computeOnClient(client -> Squirrels.comeDown(client, trunk, true)), "a bare trunk under leaves");
			context.waitFor(client -> Squirrels.settled() == 1, 20 * 15);
			look(context, world, trunk, 1.6);
			Screenshots.take(context, "cassonade-squirrels-ground");
			context.runOnClient(client -> Squirrels.sitUp());
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-squirrels-sitting");

			// A player at a distance leaves it be; one coming near sends it up into the leaves
			Vec3 foot = Vec3.atBottomCenterOf(trunk);
			tp(context, world, foot.add(0, 0, -11));
			world.getServer().runCommand("gamemode creative @a");
			context.waitTicks(20);
			check(context.computeOnClient(client -> Squirrels.count() == 1 && Squirrels.fleeing() == 0), "it stays while the player keeps away");
			tp(context, world, foot.add(0, 0, -5));
			context.waitFor(client -> Squirrels.fleeing() == 1, 10);
			context.waitTicks(1);
			Screenshots.take(context, "cassonade-squirrels-fleeing");
			context.waitFor(client -> Squirrels.count() == 0, 20 * 3);

			// A stone a block out from each side of the trunk, between spots it may go to: it bounds around it, never through
			world.getServer().runCommand("gamemode spectator @a");
			for (int side = 0; side < 4; side++) {
				BlockPos stone = trunk.offset(side == 0 ? 2 : side == 1 ? -2 : 0, 0, side == 2 ? 2 : side == 3 ? -2 : 0);
				world.getServer().runCommand(fill(stone, stone, "cobblestone"));
			}
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			check(context.computeOnClient(client -> Squirrels.comeDown(client, trunk, true)), "a bare trunk under leaves");
			for (int tick = 0; tick < 20 * 15; tick++) {
				check(context.computeOnClient(client -> {
					Vec3 at = Squirrels.first();
					if (at == null || at.y > trunk.getY() + 0.2) {
						return true;
					}
					BlockPos block = BlockPos.containing(at.x, trunk.getY() + 0.05, at.z);
					return client.level.getBlockState(block).getCollisionShape(client.level, block).isEmpty();
				}), "it stays out of the stones");
				context.waitTick();
			}
		}
	}

	private static String fill(BlockPos from, BlockPos to, String block) {
		return "fill " + from.getX() + " " + from.getY() + " " + from.getZ() + " " + to.getX() + " " + to.getY() + " " + to.getZ() + " " + block;
	}

	/** The eyes this far from the squirrel, out from the side of the trunk it is on, a little aside and above, turned towards it. */
	private static void look(ClientGameTestContext context, TestSingleplayerContext world, BlockPos trunk, double distance) {
		Vec3 squirrel = context.computeOnClient(client -> Squirrels.first());
		Vec3 out = new Vec3(squirrel.x - trunk.getX() - 0.5, 0, squirrel.z - trunk.getZ() - 0.5).normalize();
		Vec3 eye = squirrel.add(out.scale(distance)).add(-out.z * distance * 0.5, 0.1, out.x * distance * 0.5);
		look(context, world, eye, squirrel);
	}

	/** Standing here, turned towards the first squirrel. */
	private static void tp(ClientGameTestContext context, TestSingleplayerContext world, Vec3 feet) {
		look(context, world, feet.add(0, 1.62, 0), context.computeOnClient(client -> Squirrels.first()));
	}

	private static void look(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 squirrel) {
		Vec3 to = squirrel.add(0, 0.1, 0).subtract(eye);
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
