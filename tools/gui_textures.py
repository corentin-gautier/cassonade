#!/usr/bin/env python3
"""Generates the mod's GUI sprites.

- Leather backpack panel and the recipe book's ribbon.
  The rest of the GUI is styled by tools/gui_style.py. The panel is drawn at its exact size (9 x 6 slots
  and a title bar), with the slots baked in, so the leather grain and stitching are never
  stretched. Keep the sizes in sync with GearLayout.

    python3 tools/gui_textures.py
"""

import random
import struct
import zlib
from pathlib import Path

import gui_theme as theme
from gui_theme import rgb

GUI_SPRITES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/vanillasugar/textures/gui/sprites"
SPRITES = GUI_SPRITES / "container"

COLUMNS, ROWS = 9, 6
PANEL_WIDTH = 7 + COLUMNS * 18 + 7
PANEL_HEIGHT = 17 + ROWS * 18 + 7
RIBBON_WIDTH, RIBBON_HEIGHT = 7, 18

# Colors are set in tools/gui_theme.py
OUTLINE = rgb(theme.LEATHER_OUTLINE)
LEATHER = rgb(theme.LEATHER)
HIGHLIGHT = rgb(theme.LEATHER_HIGHLIGHT)
SHADOW = rgb(theme.LEATHER_SHADOW)
STITCH = rgb(theme.STITCH)
SLOT_DARK = rgb(theme.LEATHER_SLOT_DARK)
SLOT_LIGHT = rgb(theme.LEATHER_SLOT_LIGHT)
SLOT_INNER = rgb(theme.LEATHER_SLOT_INNER)
RIBBON = rgb(theme.RIBBON)
RIBBON_LIGHT = rgb(theme.RIBBON_LIGHT)
RIBBON_DARK = rgb(theme.RIBBON_DARK)



def write_png(path, pixels):
	height, width = len(pixels), len(pixels[0])
	raw = b"".join(b"\0" + bytes(c for px in row for c in px) for row in pixels)

	def chunk(kind, data):
		body = kind + data
		return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

	png = b"\x89PNG\r\n\x1a\n"
	png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
	png += chunk(b"IDAT", zlib.compress(raw, 9))
	png += chunk(b"IEND", b"")
	path.write_bytes(png)
	print(f"wrote {path.relative_to(GUI_SPRITES.parents[7])} ({width}x{height})")


def shade(color, amount):
	return tuple(max(0, min(255, c + amount)) for c in color)


def leather(width, height, base, seed):
	"""Brown base with a fine grain: per-pixel noise smoothed along rows, plus a few darker creases."""
	rng = random.Random(seed)
	pixels = []
	for _ in range(height):
		row, previous = [], 0
		for _ in range(width):
			previous = (previous + rng.randint(-9, 9)) // 2
			row.append(shade(base, previous) + (255,))
		pixels.append(row)
	for _ in range(width * height // 180):
		x, y = rng.randrange(width), rng.randrange(height)
		for step in range(rng.randint(2, 4)):
			if 0 <= x + step < width:
				r, g, b, a = pixels[y][x + step]
				pixels[y][x + step] = shade((r, g, b), -10) + (a,)
	return pixels


def frame(pixels, outline):
	"""Outline with cut corners and a one pixel bevel, like vanilla's panels."""
	height, width = len(pixels), len(pixels[0])
	for y in range(height):
		for x in range(width):
			edge_x = x in (0, width - 1)
			edge_y = y in (0, height - 1)
			if edge_x and edge_y:
				pixels[y][x] = (0, 0, 0, 0)
			elif edge_x or edge_y or (x in (1, width - 2) and y in (1, height - 2)):
				pixels[y][x] = outline + (255,)
			elif x == 1 or y == 1:
				pixels[y][x] = HIGHLIGHT + (255,)
			elif x == width - 2 or y == height - 2:
				pixels[y][x] = SHADOW + (255,)


def stitching(pixels, inset):
	"""Dashed seam running all around the panel, each stitch with a small shadow below it."""
	height, width = len(pixels), len(pixels[0])
	left, top, right, bottom = inset, inset, width - 1 - inset, height - 1 - inset
	seam = [(x, top) for x in range(left, right)] + [(right, y) for y in range(top, bottom)]
	seam += [(x, bottom) for x in range(right, left, -1)] + [(left, y) for y in range(bottom, top, -1)]
	for i, (x, y) in enumerate(seam):
		if i % 4 < 2:
			pixels[y][x] = STITCH + (255,)
			if y + 1 < height and pixels[y + 1][x][:3] != STITCH:
				pixels[y + 1][x] = SHADOW + (255,)


def slot(pixels, left, top):
	for y in range(18):
		for x in range(18):
			if x == 0 or y == 0:
				color = SLOT_DARK if x < 17 and y < 17 else SLOT_INNER
			elif x == 17 or y == 17:
				color = SLOT_LIGHT
			else:
				color = SLOT_INNER
			pixels[top + y][left + x] = color + (255,)


def panel():
	pixels = leather(PANEL_WIDTH, PANEL_HEIGHT, LEATHER, seed=54)
	frame(pixels, OUTLINE)
	stitching(pixels, inset=3)
	for row in range(ROWS):
		for column in range(COLUMNS):
			slot(pixels, 7 + column * 18, 17 + row * 18)
	return pixels


def ribbon(highlighted):
	"""Closes the recipe book: the bookmark ribbon sticking out of its top, cut in a V."""
	color = RIBBON_LIGHT if highlighted else RIBBON
	pixels = [[(0, 0, 0, 0)] * RIBBON_WIDTH for _ in range(RIBBON_HEIGHT)]
	middle = RIBBON_WIDTH // 2
	for y in range(RIBBON_HEIGHT):
		for x in range(RIBBON_WIDTH):
			if y < 3 and abs(x - middle) < 3 - y:
				continue
			if x == 0 or x == RIBBON_WIDTH - 1:
				pixels[y][x] = RIBBON_DARK + (255,)
			else:
				pixels[y][x] = (shade(color, 18) if x == 1 else shade(color, -14) if x == RIBBON_WIDTH - 2 else color) + (255,)
	return pixels


def main():
	write_png(SPRITES / "backpack_panel.png", panel())
	write_png(SPRITES / "recipe_book_ribbon.png", ribbon(False))
	write_png(SPRITES / "recipe_book_ribbon_highlighted.png", ribbon(True))


if __name__ == "__main__":
	main()
