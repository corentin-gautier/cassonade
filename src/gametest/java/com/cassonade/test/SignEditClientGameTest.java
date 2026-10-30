package com.cassonade.test;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

import com.cassonade.client.mixin.AbstractSignEditScreenAccessor;

public class SignEditClientGameTest implements FabricClientGameTest {
	// Both the control and command bits, the shortcuts reading one or the other depending on the system
	private static final int SHORTCUT = InputConstants.MOD_CONTROL | InputConstants.MOD_SUPER;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("execute at @a run setblock ~2 ~ ~ oak_sign");
			world.getServer().runCommand("execute at @a run setblock ~-2 ~ ~ spruce_sign");
			open(context, world, 2);

			ScreenInput.typeOnScreen(context, "hello");
			ScreenInput.pressOnScreen(context, InputConstants.KEY_DOWN);
			ScreenInput.typeOnScreen(context, "world");
			context.waitTicks(1);

			// The arrows keep the column between lines
			ScreenInput.pressOnScreen(context, InputConstants.KEY_LEFT);
			ScreenInput.pressOnScreen(context, InputConstants.KEY_UP);
			ScreenInput.typeOnScreen(context, "A");
			check(line(context, world, 2, 0).equals("hellAo"), "the column kept going up (" + line(context, world, 2, 0) + ")");

			// The arrows cross lines at the start and the end of one, here from the empty third line
			ScreenInput.pressOnScreen(context, InputConstants.KEY_DOWN);
			ScreenInput.pressOnScreen(context, InputConstants.KEY_DOWN);
			ScreenInput.pressOnScreen(context, InputConstants.KEY_LEFT);
			ScreenInput.typeOnScreen(context, "C");
			check(line(context, world, 2, 1).equals("worldC"), "the end of the line before (" + line(context, world, 2, 1) + ")");
			ScreenInput.pressOnScreen(context, InputConstants.KEY_RIGHT);
			ScreenInput.typeOnScreen(context, "B");
			check(line(context, world, 2, 2).equals("B"), "the start of the line after (" + line(context, world, 2, 2) + ")");

			// A click in the middle of the second line (the text centered at the middle of the window)
			mouse(context, 1);
			ScreenInput.typeOnScreen(context, "|");
			String clicked = line(context, world, 2, 1);
			check(clicked.indexOf('|') > 0 && clicked.indexOf('|') < clicked.length() - 1, "the cursor placed by the mouse (" + clicked + ")");

			// Nothing selected: copy takes the whole sign, paste fills another one
			key(context, InputConstants.KEY_C, 'c', SHORTCUT);
			String clipboard = context.computeOnClient(client -> client.keyboardHandler.getClipboard());
			check(clipboard.startsWith("hellAo\n" + clicked), "the whole sign copied (" + clipboard + ")");
			context.setScreen(() -> null);
			context.waitTicks(2);

			open(context, world, -2);
			key(context, InputConstants.KEY_V, 'v', SHORTCUT);
			context.waitTicks(1);
			check(line(context, world, -2, 0).equals("hellAo") && line(context, world, -2, 1).equals(clicked), "the whole sign pasted");

			context.setScreen(() -> null);
		}
	}

	private static void open(ClientGameTestContext context, TestSingleplayerContext world, int offset) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.openTextEdit((SignBlockEntity) player.level().getBlockEntity(player.blockPosition().east(offset)), SignTextSlot.FRONT);
		});
		context.waitForScreen(AbstractSignEditScreen.class);
		context.waitTicks(2);
	}

	/** The client's copy of the sign, updated as you type. */
	private static String line(ClientGameTestContext context, TestSingleplayerContext world, int offset, int line) {
		return context.computeOnClient(client -> {
			BlockPos pos = client.player.blockPosition().east(offset);
			return ((SignBlockEntity) client.level.getBlockEntity(pos)).getText(SignTextSlot.FRONT).getMessages(false).get(line).getString();
		});
	}

	private static void key(ClientGameTestContext context, int key, int keycode, int modifiers) {
		context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(key, keycode, modifiers)));
	}

	private static void mouse(ClientGameTestContext context, int line) {
		context.runOnClient(client -> {
			AbstractSignEditScreen screen = (AbstractSignEditScreen) client.gui.screen();
			AbstractSignEditScreenAccessor access = (AbstractSignEditScreenAccessor) screen;
			float lineY = (line - 1.5F) * access.cassonade$getSign().getTextLineHeight() * access.cassonade$getSignTextScale().y() + access.cassonade$getSignYOffset();
			MouseButtonEvent event = new MouseButtonEvent(screen.width / 2.0, lineY, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
			screen.mouseClicked(event, false);
			screen.mouseReleased(event);
		});
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
