package com.cassonade.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.MouseButtonEvent;

/** Mouse button 4 does in a menu what Esc does, unless the player gave that button another use. */
public final class MouseBack {
	private MouseBack() {
	}

	public static void init() {
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) ->
				ScreenMouseEvents.allowMouseClick(screen).register((current, event) -> {
					if (event.button() != InputConstants.MOUSE_BUTTON_4 || !current.shouldCloseOnEsc() || isBound(minecraft, event)
							// The controls screen is waiting for a button to bind
							|| current instanceof KeyBindsScreen keyBinds && keyBinds.selectedKey != null) {
						return true;
					}
					current.onClose();
					return false;
				}));
	}

	private static boolean isBound(Minecraft minecraft, MouseButtonEvent event) {
		for (KeyMapping mapping : minecraft.options.keyMappings) {
			if (mapping.matchesMouse(event)) {
				return true;
			}
		}
		return false;
	}
}
