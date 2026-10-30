package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.Direction;

import com.cassonade.client.FacingPreview;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

public class FacingPreviewClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("fill 0 100 0 8 100 8 stone");
			world.getServer().runCommand("fill 0 101 0 8 104 8 air");
			world.getServer().runCommand("fill 5 101 0 5 104 8 stone");
			// Facing the wall to the east, at eye level
			world.getServer().runCommand("tp @a 3.5 101 4.5 -90 0");

			check(hold(context, world, "stone") == null, "nothing for another block");
			check(hold(context, world, "piston").facing() == Direction.WEST, "a piston pushes back toward the player");
			check(hold(context, world, "observer").facing() == Direction.EAST, "an observer watches where the player looks");
			check(hold(context, world, "hopper").facing() == Direction.EAST, "a hopper's spout goes into the wall");
			check(hold(context, world, "piston").pos().getX() == 4, "the block in front of the wall");

			// From farther and aside, to see the face lit
			world.getServer().runCommand("tp @a 2.0 101 3.2 -60 5");
			hold(context, world, "piston");
			Screenshots.take(context, "cassonade-facing-preview-piston");
			hold(context, world, "hopper");
			Screenshots.take(context, "cassonade-facing-preview-hopper");
			world.getServer().runCommand("tp @a 1.5 101 4.5 -90 40");
			check(hold(context, world, "piston").facing() == Direction.WEST, "a piston on the ground pushes back toward the player");
			Screenshots.take(context, "cassonade-facing-preview-ground");
			world.getServer().runCommand("tp @a 3.5 101 4.5 -90 0");

			// Looking at the ground, farther than the player's feet
			world.getServer().runCommand("tp @a 3.5 101 4.5 -90 60");
			check(hold(context, world, "piston").facing() == Direction.UP, "a piston placed from above pushes up");
			check(hold(context, world, "hopper").facing() == Direction.DOWN, "a hopper on the ground goes down");

			// A chest opens rather than receiving the block
			world.getServer().runCommand("setblock 4 101 4 chest");
			check(hold(context, world, "piston") == null, "nothing on a chest, which opens instead");
		}
	}

	private static FacingPreview.Placement hold(ClientGameTestContext context, TestSingleplayerContext world, String item) {
		world.getServer().runCommand("item replace entity @a weapon.mainhand with " + item);
		context.waitTicks(5);
		return context.computeOnClient(client -> FacingPreview.current());
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
