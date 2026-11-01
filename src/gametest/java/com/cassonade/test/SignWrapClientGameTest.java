package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;
import static com.cassonade.test.PlannedWorld.player;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

public class SignWrapClientGameTest implements FabricClientGameTest {
	private static final BlockPos SIGN = new BlockPos(4, 101, 2);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runCommand("setblock 4 101 2 oak_sign");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> player(server).openTextEdit((SignBlockEntity) server.overworld().getBlockEntity(SIGN), SignTextSlot.FRONT));
			context.waitForScreen(AbstractSignEditScreen.class);
			context.waitTicks(2);

			ScreenInput.typeOnScreen(context, "this way to the mine");
			context.waitTicks(1);
			check(line(context, 0).equals("this way to the"), "the first line cut at a word: " + line(context, 0));
			check(line(context, 1).equals("mine"), "the word carried over whole: " + line(context, 1));
			ScreenInput.typeOnScreen(context, "!");
			check(line(context, 1).equals("mine!"), "typing goes on on the second line: " + line(context, 1));

			// Backspace at the start of a line goes up: joined if it fits, else only the cursor
			left(context, "mine!".length());
			ScreenInput.pressOnScreen(context, InputConstants.KEY_BACKSPACE);
			ScreenInput.typeOnScreen(context, "");
			check(line(context, 0).equals("this way to the") && line(context, 1).equals("mine!"), "too long for the line above, it stays: " + line(context, 0));
			ScreenInput.pressOnScreen(context, InputConstants.KEY_DOWN);
			ScreenInput.pressOnScreen(context, InputConstants.KEY_DOWN);
			ScreenInput.typeOnScreen(context, "go");
			ScreenInput.pressOnScreen(context, InputConstants.KEY_DOWN);
			ScreenInput.typeOnScreen(context, "on");
			left(context, "on".length());
			ScreenInput.pressOnScreen(context, InputConstants.KEY_BACKSPACE);
			context.waitTicks(1);
			check(line(context, 3).isEmpty() && line(context, 2).equals("go on"), "a short line joins the one above: " + line(context, 2) + "/" + line(context, 3));

			// One long word goes over on its own, the last line keeps vanilla's limit
			ScreenInput.pressOnScreen(context, InputConstants.KEY_DOWN);
			ScreenInput.typeOnScreen(context, "w".repeat(40));
			context.waitTicks(1);
			check(line(context, 3).length() < 40, "the end of the sign stops typing");
			context.setScreen(() -> null);
		}
	}

	private static void left(ClientGameTestContext context, int times) {
		for (int i = 0; i < times; i++) {
			ScreenInput.pressOnScreen(context, InputConstants.KEY_LEFT);
		}
	}

	private static String line(ClientGameTestContext context, int line) {
		return context.computeOnClient(client -> ((SignBlockEntity) client.level.getBlockEntity(SIGN)).getText(SignTextSlot.FRONT).getMessages(false).get(line).getString());
	}
}
