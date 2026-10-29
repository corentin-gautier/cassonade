#!/usr/bin/env python3
"""Draws the search field of the storage screens (ContainerSearch) as a small window above them: the
inventory window's edge, read from textures/gui.png so its retouches carry over, around a field sunk
like the inventory's slots. Shared by the chests, the shulker boxes and the backpack.

The sprite is a nine-slice (the .mcmeta next to it) drawn at the field's size; EditBoxMixin moves the
text inside the sunk field.

    python3 tools/search_field_texture.py && python3 tools/gui_atlas.py
"""

from pathlib import Path

import gui_atlas
import gui_theme as theme
from gui_theme import rgb
from pngio import write_png

SPRITE = Path(__file__).resolve().parent.parent / "src/main/resources/assets/cassonade/textures/gui/sprites/container/storage_search_field.png"
SOURCE = "minecraft/textures/gui/container/inventory.png"
# The inventory window's size in its 256×256 file
SOURCE_WIDTH = 176
SOURCE_HEIGHT = 166
# ContainerSearch.SEARCH_WIDTH and SEARCH_HEIGHT
WIDTH = 112
HEIGHT = 22
# Outline, lit edge and wood, then the sunk field (EditBoxMixin.STORAGE_FRAME)
FRAME = 4


def source(i, size, source_size):
	"""The window's pixel for a pixel of the field: its edges kept, the middle taken along them."""
	if i < FRAME:
		return i
	if i >= size - FRAME:
		return source_size - (size - i)
	return FRAME + (i - FRAME) % (source_size - 2 * FRAME)


def field(window):
	pixels = [[window[source(y, HEIGHT, SOURCE_HEIGHT)][source(x, WIDTH, SOURCE_WIDTH)] for x in range(WIDTH)]
		for y in range(HEIGHT)]
	left, top, right, bottom = FRAME, FRAME, WIDTH - 1 - FRAME, HEIGHT - 1 - FRAME
	for y in range(top, bottom + 1):
		for x in range(left, right + 1):
			# Like a slot: shadow along the top and left, light along the bottom and right
			shadow = x == left and y < bottom or y == top and x < right
			light = x == right and y > top or y == bottom and x > left
			color = theme.RIM_DARK if shadow else theme.WOOD_LIGHT if light else theme.FILL
			pixels[y][x] = rgb(color) + (255,)
	return pixels


def main():
	write_png(SPRITE, field(gui_atlas.unpack()[SOURCE]))


if __name__ == "__main__":
	main()
