package com.cassonade.test;

import java.lang.reflect.Field;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;

/** A window in the background draws at 30 frames per second, except with the option of limiting only a minimized one. */
public class UnfocusedWindowClientGameTest implements FabricClientGameTest {
	private static final int LIMIT = 200;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.runOnClient(client -> {
			client.options.framerateLimit().set(LIMIT);
			client.getFramerateLimitTracker().setFramerateLimit(LIMIT);
			client.getFramerateLimitTracker().onInputReceived();
			client.options.inactivityFpsLimit().set(InactivityFpsLimit.AFK);
			// Not the out of level menu's limit, which a title screen would give
			int inUse = client.getFramerateLimitTracker().getFramerateLimit();
			focus(client, false);
			check(client.getFramerateLimitTracker().getFramerateLimit() == 30, "a window in the background draws at 30 frames per second");
			client.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
			check(client.getFramerateLimitTracker().getFramerateLimit() == inUse, "the option of limiting only a minimized window");
			client.options.inactivityFpsLimit().set(InactivityFpsLimit.AFK);
			focus(client, true);
			check(client.getFramerateLimitTracker().getFramerateLimit() == inUse, "a window in use is not limited");
		});
	}

	/** Written directly: the test API keeps the window focused, ignoring GLFW's callback. */
	private static void focus(Minecraft client, boolean focused) {
		try {
			Field field = Window.class.getDeclaredField("focused");
			field.setAccessible(true);
			field.setBoolean(client.getWindow(), focused);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
