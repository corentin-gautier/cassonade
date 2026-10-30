package com.cassonade.test;

import java.nio.file.Path;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import net.minecraft.util.Util;

import com.cassonade.client.ClosingScreen;
import com.cassonade.client.OpenFade;

/** Screenshots taken once the windows are done opening or closing: those animate in real time, not in ticks. */
final class Screenshots {
	/** A little over the windows' animation (SLIDE_MILLIS in ScreenMixin), for those closing. */
	private static final long ANIMATION_MS = 200;

	private Screenshots() {
	}

	static Path take(ClientGameTestContext context, String name) {
		// Waited for in ticks, which keep the frames coming, rather than slept: a window not drawn yet would only start
		// opening once the sleep is over. Nothing to wait for in most shots, where nothing moves.
		context.waitFor(client -> !(client.gui.screen() instanceof ClosingScreen screen && !screen.settled())
				&& Util.getMillis() - OpenFade.closedAt() >= ANIMATION_MS, 20 * 30);
		return context.takeScreenshot(name);
	}
}
