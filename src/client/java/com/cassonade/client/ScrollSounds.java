package com.cassonade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * A soft tick each time a list moves by a step, whether by the wheel or by dragging its scrollbar, like a ratchet: the
 * game's button click, higher and much quieter. Only what the player does ticks, not a list scrolled by the game
 * (opened on a row, reset on a new tab). A fast drag across many steps ticks at most once every
 * {@link #MIN_INTERVAL} milliseconds, so it whirs rather than rattles.
 */
public final class ScrollSounds {
	private static final float PITCH = 1.9F;
	private static final float PITCH_SPREAD = 0.1F;
	private static final float VOLUME = 0.12F;
	private static final long MIN_INTERVAL = 45;
	private static final RandomSource RANDOM = RandomSource.create();

	private static long last;

	private ScrollSounds() {
	}

	/** A list scrolled from one step, row or page index to another. */
	public static void moved(int before, int after) {
		if (before != after) {
			tick();
		}
	}

	/**
	 * A list scrolled by pixels, from one place to another between 0 and max, a step being so many pixels. Reaching an
	 * end ticks too, as the last move there is often shorter than a step.
	 */
	public static void moved(double before, double after, double step, double max) {
		if (step <= 0 || before == after) {
			return;
		}
		if (Math.floor(before / step) != Math.floor(after / step) || after <= 0 || after >= max) {
			tick();
		}
	}

	private static void tick() {
		long now = Util.getMillis();
		if (now - last < MIN_INTERVAL || !Settings.on(Setting.INTERFACE_SOUNDS)) {
			return;
		}
		last = now;
		float pitch = PITCH + (RANDOM.nextFloat() - 0.5F) * PITCH_SPREAD;
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, VOLUME));
	}
}
