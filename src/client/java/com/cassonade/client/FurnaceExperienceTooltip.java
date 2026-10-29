package com.cassonade.client;

import java.util.Locale;

import com.cassonade.furnace.FurnaceExperience;

/**
 * The experience a furnace holds, under its output slot (AbstractFurnaceScreenMixin). It comes from the server
 * (FurnaceExperience) for the window that is open: on a server without the mod, nothing shows.
 */
public final class FurnaceExperienceTooltip {
	private static int containerId = -1;
	private static int tenths;

	private FurnaceExperienceTooltip() {
	}

	public static void receive(FurnaceExperience experience) {
		containerId = experience.containerId();
		tenths = experience.tenths();
	}

	public static void clear() {
		containerId = -1;
	}

	/** The experience held, in tenths, in the window with this id; 0 if the server did not say. */
	public static int tenths(int menuId) {
		return menuId == containerId ? tenths : 0;
	}

	/** Whole numbers as they are, others to one decimal: a raw iron ingot gives 0.7. */
	public static String format(int tenths) {
		return tenths % 10 == 0 ? Integer.toString(tenths / 10) : String.format(Locale.ROOT, "%.1f", tenths / 10.0);
	}
}
