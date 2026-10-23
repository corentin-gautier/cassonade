#!/usr/bin/env python3
"""Draws vanilla's panels in the minimap's frame, like the rest of the HUD: its wooden border around the
slots' dark fill. The frame is read from textures/gui.png, so its retouches carry over.

- The frames of the status effects listed next to the inventory, like the effects on the HUD
  (EffectsHud), and for the effects a beacon gives, a background tinted like its beam. Keep the tint in
  sync with EffectsHud.AMBIENT_BACKGROUND.
- The toasts in the top right corner: new recipes, tutorial hints, system messages, the music playing.
  Their nine-slice borders match the frame's (the .mcmeta files next to them); their text is lightened
  by ToastTextMixin.

    python3 tools/panel_textures.py
"""

from pathlib import Path

import gui_atlas
import gui_theme as theme
from gui_theme import rgb
from pngio import write_png

SPRITES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/minecraft/textures/gui/sprites"
FRAME = "vanillasugar/textures/gui/sprites/hud/minimap_frame.png"
FRAME_SIZE = 16
BORDER = 4
AMBIENT = "#1E3438"
TOASTS = ["recipe", "tutorial", "system", "now_playing", "advancement"]
TOAST_WIDTH = 160
TOAST_HEIGHT = 32


def source(i, size):
	"""The frame's pixel for a pixel of the larger sprite: the corners and edges kept, the middle repeated."""
	if i < BORDER:
		return i
	if i >= size - BORDER:
		return FRAME_SIZE - (size - i)
	return BORDER + (i - BORDER) % (FRAME_SIZE - 2 * BORDER)


def panel(frame, width, height, fill):
	pixels = [[frame[source(y, height)][source(x, width)] for x in range(width)] for y in range(height)]
	for y in range(height):
		for x in range(width):
			if pixels[y][x][3] == 0:
				pixels[y][x] = fill + (255,)
	return pixels


def main():
	frame = gui_atlas.unpack()[FRAME]
	inventory = SPRITES / "container/inventory"
	inventory.mkdir(parents=True, exist_ok=True)
	write_png(inventory / "effect_background.png", panel(frame, 32, 32, rgb(theme.FILL)))
	write_png(inventory / "effect_background_ambient.png", panel(frame, 32, 32, rgb(AMBIENT)))
	toast = SPRITES / "toast"
	toast.mkdir(parents=True, exist_ok=True)
	for name in TOASTS:
		write_png(toast / f"{name}.png", panel(frame, TOAST_WIDTH, TOAST_HEIGHT, rgb(theme.FILL)))


if __name__ == "__main__":
	main()
