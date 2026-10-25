package com.cassonade.client.spyglass;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Looking through a spyglass, the scroll wheel zooms one step further in, or one step further out, than the
 * game's own zoom (MouseHandlerMixin), instead of changing the item in hand, which would put the spyglass
 * down. Back to the game's zoom each time the spyglass is raised again.
 */
public final class ScopeZoom {
	/** The field of view's scale at each step, the game's own in the middle. */
	private static final float[] FIELD_OF_VIEW = {0.2F, 0.1F, 0.05F};
	/** The closest step, below which the camera would otherwise stop at the game's (CameraMixin). */
	public static final float NARROWEST = 0.05F;
	private static final int GAME_STEP = 1;

	private static int step = GAME_STEP;

	private ScopeZoom() {
	}

	/** One step in for the wheel turned up, out for down, the spyglass's sound a little higher or lower. */
	public static void scroll(Minecraft minecraft, double wheel) {
		int next = Mth.clamp(step + (wheel > 0 ? 1 : -1), 0, FIELD_OF_VIEW.length - 1);
		if (next != step) {
			step = next;
			minecraft.player.playSound(SoundEvents.SPYGLASS_USE, 0.5F, 0.8F + 0.2F * step);
		}
	}

	public static float fieldOfView() {
		return FIELD_OF_VIEW[step];
	}

	public static void tick(Minecraft minecraft) {
		if (minecraft.player == null || !minecraft.player.isScoping()) {
			step = GAME_STEP;
		}
	}
}
