package com.cassonade.test;

import java.nio.file.Path;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Screenshots taken once the windows are done opening or closing: those animate in real time, not in ticks. */
final class Screenshots {
	/** A little over the windows' animation (SLIDE_MILLIS in ScreenMixin). */
	private static final long ANIMATION_MS = 200;

	private Screenshots() {
	}

	static Path take(ClientGameTestContext context, String name) {
		try {
			Thread.sleep(ANIMATION_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
		return context.takeScreenshot(name);
	}
}
