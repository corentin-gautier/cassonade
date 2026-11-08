package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Robins;
import com.cassonade.client.ambience.Sparrows;

/**
 * Sparrows perch on what was built: a fence's post, a wall, a roof's stairs and slabs, never on grass; they stay while the player
 * keeps away, fly off as they come near, and come on their own around a little house.
 */
public class SparrowsClientGameTest implements FabricClientGameTest {
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
				Sparrows.setSpawning(false);
			});
			world.getServer().runCommand("gamerule send_command_feedback false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 6000");
			BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
			world.getServer().runCommand(fill(feet.offset(-16, 0, -16), feet.offset(16, 24, 24), "air"));
			world.getServer().runCommand(fill(feet.offset(-16, -1, -16), feet.offset(16, -1, 24), "grass_block"));
			// A fence along the path, a wall's post, and a little house with a stair roof and a slab porch
			BlockPos fence = feet.offset(-3, 0, 8);
			world.getServer().runCommand(fill(fence, fence.offset(6, 0, 0), "oak_fence"));
			BlockPos wall = feet.offset(5, 0, 6);
			world.getServer().runCommand(fill(wall, wall, "cobblestone_wall"));
			BlockPos house = feet.offset(-2, 0, 12);
			world.getServer().runCommand(fill(house, house.offset(4, 2, 4), "oak_planks"));
			world.getServer().runCommand(fill(house.offset(-1, 3, -1), house.offset(5, 3, 5), "spruce_stairs[facing=south]"));
			BlockPos porch = house.offset(2, 0, -1);
			world.getServer().runCommand(fill(porch, porch, "stone_slab"));
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(5);

			check(context.computeOnClient(client -> !Sparrows.perch(client, feet.offset(0, -1, 3))), "none on grass");
			check(context.computeOnClient(client -> Sparrows.perch(client, fence.offset(3, 0, 0))), "one on a fence post");
			check(context.computeOnClient(client -> Sparrows.perch(client, wall)), "one on a wall");
			check(context.computeOnClient(client -> Sparrows.perch(client, house.offset(2, 3, 0))), "one on the roof");
			check(context.computeOnClient(client -> Sparrows.perch(client, porch)), "one on the porch's slab");

			// Up close, unseen by them
			world.getServer().runCommand("gamemode spectator @a");
			look(context, world, new Vec3(fence.getX() + 4.6, fence.getY() + 1.9, fence.getZ() - 1.4), Vec3.atBottomCenterOf(fence.offset(3, 1, 0)));
			Screenshots.take(context, "cassonade-sparrows-fence");
			look(context, world, new Vec3(house.getX() + 4.2, house.getY() + 5, house.getZ() - 2.2), Vec3.atBottomCenterOf(house.offset(2, 4, 0)));
			Screenshots.take(context, "cassonade-sparrows-roof");
			look(context, world, new Vec3(house.getX() + 9, house.getY() + 4, house.getZ() - 5), Vec3.atBottomCenterOf(house.offset(2, 4, 0)));
			Screenshots.take(context, "cassonade-sparrows-house");

			// A player at a distance leaves them be; one coming near sends the fence's off
			world.getServer().runCommand("gamemode creative @a");
			look(context, world, new Vec3(fence.getX() + 3.5, feet.getY() + 1.62, fence.getZ() - 12), Vec3.atBottomCenterOf(fence.offset(3, 1, 0)));
			context.waitTicks(20);
			check(context.computeOnClient(client -> Sparrows.count() == 4 && Sparrows.flying() == 0), "they stay while the player keeps away");
			look(context, world, new Vec3(fence.getX() + 3.5, feet.getY() + 1.62, fence.getZ() - 3), Vec3.atBottomCenterOf(fence.offset(3, 1, 0)));
			context.waitFor(client -> Sparrows.flying() >= 1, 10);

			// One on a post against the house's wall, the player coming from the other side: it flies off around the house, never through it
			context.runOnClient(client -> Sparrows.setSpawning(false));
			BlockPos post = house.offset(5, 0, 2);
			world.getServer().runCommand(fill(post, post, "oak_fence"));
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(3);
			check(context.computeOnClient(client -> Sparrows.perch(client, post)), "one on the post by the wall");
			look(context, world, new Vec3(post.getX() + 3.5, feet.getY() + 1.62, post.getZ() + 0.5), Vec3.atBottomCenterOf(post.above()));
			context.waitFor(client -> Sparrows.flying() == 1, 20);
			for (int tick = 0; tick < 60; tick++) {
				check(context.computeOnClient(client -> !Sparrows.anyInBlock()), "the sparrow out of the blocks as it flies off, tick " + tick);
				context.waitTicks(1);
			}

			// On their own, around the house by day
			context.runOnClient(client -> Sparrows.setSpawning(true));
			world.getServer().runCommand("gamemode spectator @a");
			look(context, world, new Vec3(house.getX() + 2.5, feet.getY() + 1.62, house.getZ() - 14), Vec3.atBottomCenterOf(house.offset(2, 4, 0)));
			context.waitFor(client -> Sparrows.count() > 0 && Sparrows.flying() == 0, 20 * 60);
		}
	}

	private static String fill(BlockPos from, BlockPos to, String block) {
		return "fill " + from.getX() + " " + from.getY() + " " + from.getZ() + " " + to.getX() + " " + to.getY() + " " + to.getZ() + " " + block;
	}

	/** The eyes here, turned towards that spot. */
	private static void look(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 at) {
		Vec3 to = at.add(0, 0.15, 0).subtract(eye);
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
