#!/usr/bin/env python3
"""Draws the recipe book's slots for a result with several recipes: the slot of a single recipe, as the atlas has
it, its bottom right corner folded over and a second card peeking out behind it, like a pile.

    python3 tools/recipe_slot_textures.py

The slots are read from the atlas as the build unpacks it (run ./gradlew build once first); the new ones are
written under src/main/resources, to be packed into the atlas with python3 tools/gui_atlas.py.
"""

from pathlib import Path

from pngio import read_png, write_png

ROOT = Path(__file__).resolve().parent.parent
UNPACKED = ROOT / "build/generated/guiTextures/assets/minecraft/textures/gui/sprites/recipe_book"
TARGET = ROOT / "src/main/resources/assets/minecraft/textures/gui/sprites/recipe_book"
# The cream of the slots' lines (tools/gui_theme.py's paper shade the hatching is drawn in)
LINE = (0xC9, 0xB4, 0x7F, 255)
# The card behind shows along the slot's right and bottom edges, one pixel out, from this far in
FROM = 3
# The folded corner, this many pixels along each edge inside the frame, a shade darker than the lines so
# it stands out of the hatching of a recipe missing ingredients
FOLD = 8
FOLD_COLOR = (0xA8, 0x93, 0x5F, 255)


def pile(slot):
	pixels = [list(row) for row in slot]
	last = len(pixels) - 1
	for i in range(FROM, last + 1):
		pixels[i][last] = LINE
		pixels[last][i] = LINE
	# The frame runs one pixel in from the sprite's edge: the fold fills the corner inside it
	inner = last - 1
	for y in range(inner - FOLD + 1, inner + 1):
		for x in range(inner - FOLD + 1, inner + 1):
			if x + y >= 2 * inner - FOLD + 1:
				pixels[y][x] = FOLD_COLOR
	return pixels


def main():
	TARGET.mkdir(parents=True, exist_ok=True)
	for craftable in ("craftable", "uncraftable"):
		slot = read_png((UNPACKED / f"slot_{craftable}.png").read_bytes())
		write_png(TARGET / f"slot_many_{craftable}.png", pile(slot))


if __name__ == "__main__":
	main()
