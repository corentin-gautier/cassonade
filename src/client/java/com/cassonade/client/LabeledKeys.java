package com.cassonade.client;

import java.util.HashSet;
import java.util.Set;

import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLScancode;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import com.cassonade.client.mixin.KeyMappingAccessor;

/**
 * Key mappings are physical positions on a QWERTY keyboard, so a default of M is the comma key on
 * AZERTY. Our keys are named after what is printed on them (M for map, B for backpack, N for
 * markers, R to rotate the minimap, + and - to zoom): once SDL knows the keyboard layout, their defaults move to the key
 * printed with that character. When that key already does something else, the next character is
 * tried: + is the command key on a Mac AZERTY, so zooming in takes ) next to -; - is the hotbar's 6
 * on a PC AZERTY, so zooming out falls back to the keypad. Keys the player changed are left alone;
 * one this class picked in an earlier version counts as unchanged.
 */
public final class LabeledKeys {
	private static boolean resolved;
	/** Keys given to our mappings during this pass, so two of them never share one. */
	private static final Set<Integer> assigned = new HashSet<>();

	private LabeledKeys() {
	}

	public static void tick(Minecraft minecraft) {
		if (resolved || SDLInit.SDL_WasInit(SDLInit.SDL_INIT_VIDEO) == 0) {
			return;
		}
		resolved = true;
		label(minecraft, CassonadeClient.OPEN_WORLD_MAP, SDLScancode.SDL_SCANCODE_UNKNOWN, 'm');
		label(minecraft, CassonadeClient.OPEN_BACKPACK, SDLScancode.SDL_SCANCODE_UNKNOWN, 'b');
		label(minecraft, CassonadeClient.MARKER_DISPLAY, SDLScancode.SDL_SCANCODE_UNKNOWN, 'n');
		label(minecraft, CassonadeClient.MINIMAP_ROTATION, SDLScancode.SDL_SCANCODE_UNKNOWN, 'r');
		label(minecraft, CassonadeClient.OPEN_JOURNAL, SDLScancode.SDL_SCANCODE_UNKNOWN, 'j');
		label(minecraft, CassonadeClient.QUEST_TRACKER, SDLScancode.SDL_SCANCODE_UNKNOWN, 'k');
		label(minecraft, CassonadeClient.ZOOM_IN, SDLScancode.SDL_SCANCODE_KP_PLUS, '+', ')');
		label(minecraft, CassonadeClient.ZOOM_OUT, SDLScancode.SDL_SCANCODE_KP_MINUS, '-');
		KeyMapping.resetMapping();
	}

	/** Moves the mapping's default to the first free key printed with one of the characters, else to the fallback. */
	private static void label(Minecraft minecraft, KeyMapping mapping, int fallback, char... characters) {
		boolean unchanged = mapping.isDefault() || mapping.saveString().equals(keyName(fallback));
		int scancode = fallback;
		for (char character : characters) {
			int candidate = SDLKeyboard.SDL_GetScancodeFromKey(character, null);
			unchanged |= candidate != SDLScancode.SDL_SCANCODE_UNKNOWN && mapping.saveString().equals(keyName(candidate));
			if (candidate != SDLScancode.SDL_SCANCODE_UNKNOWN && !isTaken(minecraft, candidate)) {
				scancode = candidate;
				break;
			}
		}
		if (scancode == SDLScancode.SDL_SCANCODE_UNKNOWN) {
			return;
		}
		assigned.add(scancode);
		InputConstants.Key key = InputConstants.Type.KEYBOARD.getOrCreate(scancode);
		if (unchanged) {
			mapping.setKey(key);
		}
		((KeyMappingAccessor) mapping).cassonade$setDefaultKey(key);
	}

	/**
	 * The mapping's key as printed on the player's keyboard: the game names a letter or symbol key
	 * after its QWERTY position, so M on AZERTY would read ";".
	 */
	public static Component printedName(KeyMapping mapping) {
		InputConstants.Key key = ((KeyMappingAccessor) mapping).cassonade$getKey();
		if (key.getType() == InputConstants.Type.KEYBOARD && SDLInit.SDL_WasInit(SDLInit.SDL_INIT_VIDEO) != 0) {
			String name = SDLKeyboard.SDL_GetKeyName(SDLKeyboard.SDL_GetKeyFromScancode(key.getValue(), (short) 0, false));
			if (name != null && name.codePointCount(0, name.length()) == 1) {
				return Component.literal(name);
			}
		}
		return mapping.getTranslatedKeyMessage();
	}

	private static String keyName(int scancode) {
		return scancode == SDLScancode.SDL_SCANCODE_UNKNOWN ? "" : InputConstants.Type.KEYBOARD.getOrCreate(scancode).getName();
	}

	/**
	 * Whether another mapping already uses the key by default. Our own mappings are being moved in this
	 * pass and debug keys only work with F3 held, so neither counts.
	 */
	private static boolean isTaken(Minecraft minecraft, int scancode) {
		if (assigned.contains(scancode)) {
			return true;
		}
		InputConstants.Key key = InputConstants.Type.KEYBOARD.getOrCreate(scancode);
		for (KeyMapping other : minecraft.options.keyMappings) {
			if (other.getCategory() != CassonadeClient.CATEGORY && other.getCategory() != KeyMapping.Category.DEBUG
					&& other.getDefaultKey().equals(key)) {
				return true;
			}
		}
		return false;
	}
}
