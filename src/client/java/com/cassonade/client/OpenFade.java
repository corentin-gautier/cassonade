package com.cassonade.client;

import net.minecraft.util.ARGB;

/**
 * The opacity of what a window draws while it opens (see ScreenMixin); 1 the rest of the time.
 * Items are drawn by the game without any tint: they appear as the window settles.
 */
public final class OpenFade {
	private static float alpha = 1f;

	/** How much of the menu blur is wanted in the frame being drawn: the blur is applied after the whole screen is drawn. */
	private static float blur = 1f;

	/** When a window last started closing (Util.getMillis): it keeps being drawn a moment after the game is back. */
	private static long closedAt;

	private OpenFade() {
	}

	public static void set(float value) {
		alpha = value;
	}

	public static void setBlur(float value) {
		blur = value;
	}

	/** Read once per frame, where the blur is applied: back to full for the frames that follow. */
	public static float takeBlur() {
		float value = blur;
		blur = 1f;
		return value;
	}

	public static void closing(long now) {
		closedAt = now;
	}

	public static long closedAt() {
		return closedAt;
	}

	public static float alpha() {
		return alpha;
	}

	public static int apply(int color) {
		return alpha >= 1f ? color : ARGB.color(Math.round(ARGB.alpha(color) * alpha), color);
	}
}
