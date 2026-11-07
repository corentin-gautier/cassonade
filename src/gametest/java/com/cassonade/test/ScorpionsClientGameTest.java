package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Scorpions;
import com.cassonade.weather.SandLayerBlock;

/**
 * A scorpion comes up out of the sand and wanders about while the player keeps away; it rears up as they come near, and
 * strikes at the air and digs back in as they come closer still. It never walks off the sand.
 */
public class ScorpionsClientGameTest implements FabricClientGameTest {
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
				Scorpions.setSpawning(false);
			});
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 6000");
			// A patch of sand ringed with stone, a dead bush in it, and drifts of sand over its west half, deeper at its edge
			BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
			BlockPos center = feet.offset(0, 0, 8);
			world.getServer().runCommand(fill(feet.offset(-12, 0, -12), feet.offset(12, 16, 20), "air"));
			world.getServer().runCommand(fill(feet.offset(-12, -1, -12), feet.offset(12, -1, 20), "stone"));
			world.getServer().runCommand(fill(center.offset(-3, -1, -3), center.offset(3, -1, 3), "sand"));
			world.getServer().runCommand(fill(center.offset(2, 0, 2), center.offset(2, 0, 2), "dead_bush"));
			world.getServer().runCommand(fill(center.offset(-3, 0, -3), center.offset(-1, 0, 3), "cassonade:sand_layer[layers=2]"));
			world.getServer().runCommand(fill(center.offset(-3, 0, -3), center.offset(-3, 0, 3), "cassonade:sand_layer[layers=4]"));
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(5);

			// Up out of the sand, seen from up close, unseen
			world.getServer().runCommand("gamemode spectator @a");
			check(!context.computeOnClient(client -> Scorpions.comeUp(client, feet.getX(), feet.getZ())), "no scorpion on stone");
			check(context.computeOnClient(client -> Scorpions.comeUp(client, center.getX(), center.getZ())), "sand open to the sky");
			context.waitFor(client -> Scorpions.roaming() == 1, 20 * 3);
			look(context, world, context.computeOnClient(client -> Scorpions.first()).add(1, 0.6, 0.8));
			Screenshots.take(context, "cassonade-scorpions-roaming");

			// It wanders, always on the sand, and on the drifts, never under them
			for (int tick = 0; tick < 20 * 20; tick++) {
				check(context.computeOnClient(client -> {
					Vec3 at = Scorpions.first();
					return at != null && Math.abs(at.x - center.getX() - 0.5) < 3.5 && Math.abs(at.z - center.getZ() - 0.5) < 3.5;
				}), "it stays on the sand");
				check(context.computeOnClient(client -> onTop(client.level, Scorpions.first())), "it walks on top of the drifts");
				context.waitTick();
			}

			// Up out of a drift, on top of it
			context.runOnClient(client -> Scorpions.setSpawning(false));
			check(context.computeOnClient(client -> Scorpions.comeUp(client, center.getX() - 3, center.getZ())), "a drift of sand");
			check(context.computeOnClient(client -> Math.abs(Scorpions.first().y - (center.getY() + 0.5)) < 0.01), "on top of a drift four layers deep");
			context.runOnClient(client -> Scorpions.setSpawning(false));
			check(context.computeOnClient(client -> Scorpions.comeUp(client, center.getX(), center.getZ())), "sand open to the sky");
			context.waitFor(client -> Scorpions.roaming() == 1, 20 * 3);

			// A player at a distance leaves it be; one coming near finds it rearing up, closer still it strikes and digs in
			world.getServer().runCommand("gamemode creative @a");
			Vec3 scorpion = context.computeOnClient(client -> Scorpions.first());
			look(context, world, scorpion.add(0, 1.62, -5));
			context.waitTicks(20);
			check(context.computeOnClient(client -> Scorpions.count() == 1 && Scorpions.rearing() == 0), "it keeps calm while the player keeps away");
			scorpion = context.computeOnClient(client -> Scorpions.first());
			look(context, world, scorpion.add(0, 1.62, -1.6));
			context.waitFor(client -> Scorpions.rearing() == 1, 20);
			context.waitTicks(8);
			Screenshots.take(context, "cassonade-scorpions-rearing");
			scorpion = context.computeOnClient(client -> Scorpions.first());
			look(context, world, scorpion.add(0, 1.62, -0.7));
			context.waitFor(client -> Scorpions.leaving() == 1, 10);
			context.waitFor(client -> Scorpions.count() == 0, 20 * 2);
		}
	}

	/** Standing on the top of the sand or of the drift it is on: not sunk into the drift, nor floating over it. */
	private static boolean onTop(Level level, Vec3 at) {
		BlockPos under = BlockPos.containing(at.x, at.y - 0.01, at.z);
		BlockState state = level.getBlockState(under);
		double top = under.getY() + state.getShape(level, under).max(Direction.Axis.Y);
		// A layer fills the bottom of its block: standing on it is being in that block, no higher than its top
		BlockPos in = BlockPos.containing(at.x, at.y + 0.01, at.z);
		BlockState around = level.getBlockState(in);
		boolean buried = around.getBlock() instanceof SandLayerBlock && in.getY() + around.getShape(level, in).max(Direction.Axis.Y) > at.y + 0.01;
		return Math.abs(at.y - top) < 0.01 && !buried;
	}

	private static String fill(BlockPos from, BlockPos to, String block) {
		return "fill " + from.getX() + " " + from.getY() + " " + from.getZ() + " " + to.getX() + " " + to.getY() + " " + to.getZ() + " " + block;
	}

	/** The eyes here, turned towards the scorpion. */
	private static void look(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye) {
		Vec3 scorpion = context.computeOnClient(client -> Scorpions.first());
		Vec3 to = scorpion.add(0, 0.1, 0).subtract(eye);
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
