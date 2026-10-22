package com.vanillasugar.client;

import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;

import com.vanillasugar.VanillaSugar;

/**
 * Container windows are tinted after their block by the textures from tools/container_themes.py.
 * Blocks sharing a vanilla window (barrel and ender chest with the chest, dropper with the
 * dispenser) are told apart by the window's title; a container renamed in an anvil keeps the
 * shared texture.
 */
public final class ContainerThemes {
	private static final Map<String, Identifier> TEXTURES = Map.of(
			"container.barrel", texture("barrel"),
			"container.enderchest", texture("ender_chest"),
			"container.dropper", texture("dropper"));

	private ContainerThemes() {
	}

	/** The texture of the block this window belongs to, or the vanilla one it shares. */
	public static Identifier texture(Component title, Identifier vanilla) {
		return title.getContents() instanceof TranslatableContents translatable
				? TEXTURES.getOrDefault(translatable.getKey(), vanilla)
				: vanilla;
	}

	private static Identifier texture(String name) {
		return VanillaSugar.id("textures/gui/container/" + name + ".png");
	}
}
