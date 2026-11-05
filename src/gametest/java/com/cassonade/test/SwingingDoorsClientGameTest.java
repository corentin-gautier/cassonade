package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;

import com.cassonade.client.SwingingDoors;

/** A door, a trapdoor and a fence gate swing open and shut, then stand in their section's mesh again. */
public class SwingingDoorsClientGameTest implements FabricClientGameTest {
	private static final BlockPos DOOR = new BlockPos(0, 100, 3);
	private static final BlockPos TRAPDOOR = new BlockPos(2, 100, 3);
	private static final BlockPos GATE = new BlockPos(4, 100, 3);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("fill -6 99 -6 10 99 10 stone");
			world.getServer().runCommand("fill -6 100 -6 10 106 10 air");
			world.getServer().runCommand("kill @e[type=!player]");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("tp @a 2.5 101.2 -0.5 0 15");
			set("closed", world, false);
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> client.gui.hud.toggle());

			set("opening", world, true);
			context.waitFor(client -> SwingingDoors.openness(DOOR) > 0, 40);
			Screenshots.take(context, "swinging-doors-start");
			context.waitFor(client -> SwingingDoors.openness(DOOR) >= 0.6F, 40);
			check(context.computeOnClient(client -> SwingingDoors.swinging(DOOR) && SwingingDoors.swinging(DOOR.above())
					&& SwingingDoors.swinging(TRAPDOOR) && SwingingDoors.swinging(GATE)), "the door, trapdoor and gate swinging open");
			Screenshots.take(context, "swinging-doors-opening");
			context.waitTicks(10);
			check(context.computeOnClient(client -> !SwingingDoors.swinging(DOOR) && !SwingingDoors.swinging(TRAPDOOR) && !SwingingDoors.swinging(GATE)),
					"done swinging after a few ticks");
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "swinging-doors-open");

			set("closing", world, false);
			context.waitFor(client -> SwingingDoors.openness(DOOR) >= 0 && SwingingDoors.openness(DOOR) <= 0.4F, 40);
			Screenshots.take(context, "swinging-doors-closing");
			context.waitTicks(10);
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "swinging-doors-closed");
		}
	}

	private static void set(String what, TestSingleplayerContext world, boolean open) {
		world.getServer().runCommand("setblock 0 100 3 oak_door[half=lower,facing=north,open=" + open + "]");
		world.getServer().runCommand("setblock 0 101 3 oak_door[half=upper,facing=north,open=" + open + "]");
		world.getServer().runCommand("setblock 2 100 3 oak_trapdoor[half=bottom,facing=north,open=" + open + "]");
		world.getServer().runCommand("setblock 4 100 3 oak_fence_gate[facing=north,open=" + open + "]");
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
