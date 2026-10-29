package com.cassonade.client.trader;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import com.cassonade.trader.TraderTimePayload;

/** The server's wandering-trader timer while its merchant screen is open: vanilla pauses it while trading, so it is shown as is. */
public final class TraderTimePanel {
	private static @Nullable TraderTimePayload answer;

	private TraderTimePanel() {
	}

	public static void receive(Minecraft minecraft, TraderTimePayload payload) {
		if (minecraft.gui.screen() instanceof MerchantScreen screen && screen.getMenu().containerId == payload.containerId()) {
			answer = payload;
		}
	}

	public static void tick(Minecraft minecraft) {
		if (answer != null && (!(minecraft.gui.screen() instanceof MerchantScreen screen)
				|| screen.getMenu().containerId != answer.containerId())) {
			clear();
		}
	}

	public static void clear() {
		answer = null;
	}

	public static @Nullable Component line(int containerId) {
		if (answer == null || answer.containerId() != containerId) {
			return null;
		}
		int seconds = Mth.ceil(answer.ticks() / 20f);
		String time = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
		return Component.translatable("gui.cassonade.trader.leaves_in", time);
	}

	public static String shown() {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.gui.screen() instanceof MerchantScreen screen)) {
			return "";
		}
		Component line = line(screen.getMenu().containerId);
		return line == null ? "" : line.getString();
	}
}
