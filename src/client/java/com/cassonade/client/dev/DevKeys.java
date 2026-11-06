package com.cassonade.client.dev;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import com.cassonade.client.CassonadeClient;

/**
 * Keys for the development commands, in runClient only: those without an argument run at once, the others open the
 * chat ready for one, with its suggestions; F12 switches the mod's rendering off and on (DevRendering). Rebindable in the controls with the mod's keys. Their names are written
 * out rather than translated, as for the commands' messages: players never see them.
 */
public final class DevKeys {
	private static KeyMapping kit;
	private static KeyMapping unequip;
	private static KeyMapping regen;
	private static KeyMapping ambience;
	private static KeyMapping travel;
	private static KeyMapping vanilla;

	private DevKeys() {
	}

	public static void init() {
		kit = key("Dev: /dev kit", InputConstants.KEY_F6);
		unequip = key("Dev: /dev unequip", InputConstants.KEY_F7);
		regen = key("Dev: /dev regen (again to cancel)", InputConstants.KEY_F8);
		ambience = key("Dev: /ambience …", InputConstants.KEY_F9);
		travel = key("Dev: /dev goto …", InputConstants.KEY_F10);
		vanilla = key("Dev: vanilla rendering (again to restore)", InputConstants.KEY_F12);
		ClientTickEvents.END_CLIENT_TICK.register(DevKeys::tick);
	}

	private static KeyMapping key(String name, int key) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, key, CassonadeClient.CATEGORY));
	}

	private static void tick(Minecraft minecraft) {
		if (minecraft.player == null) {
			return;
		}
		while (kit.consumeClick()) {
			minecraft.player.connection.sendCommand("dev kit");
		}
		while (unequip.consumeClick()) {
			minecraft.player.connection.sendCommand("dev unequip");
		}
		while (regen.consumeClick()) {
			minecraft.player.connection.sendCommand("dev regen");
		}
		while (ambience.consumeClick()) {
			minecraft.gui.setScreen(new ChatScreen("/ambience ", false));
		}
		while (travel.consumeClick()) {
			minecraft.gui.setScreen(new ChatScreen("/dev goto ", false));
		}
		while (vanilla.consumeClick()) {
			DevRendering.toggle(minecraft);
		}
	}
}
