#!/usr/bin/env python3
"""Generates the mod's GUI sprites.

- Leather backpack panel and its toggle button. The panel is drawn at its exact size (9 x 6 slots
  and a title bar), with the slots baked in, so the leather grain and stitching are never
  stretched. Keep the sizes in sync with GearLayout.
- Metal frame of the minimap, a nine-slice sprite. Keep its border in sync with Minimap.BORDER.

    python3 tools/gui_textures.py
"""

import random
import struct
import zlib
from pathlib import Path

GUI_SPRITES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/vanillasugar/textures/gui/sprites"
SPRITES = GUI_SPRITES / "container"

COLUMNS, ROWS = 9, 6
PANEL_WIDTH = 7 + COLUMNS * 18 + 7
PANEL_HEIGHT = 17 + ROWS * 18 + 7
TAB_SIZE = 22
CLOSE_SIZE = 11

OUTLINE = (0x2B, 0x18, 0x0C)
LEATHER = (0x8A, 0x56, 0x2E)
LEATHER_LIGHT = (0x9E, 0x68, 0x3A)
HIGHLIGHT = (0xB4, 0x7E, 0x4C)
SHADOW = (0x5C, 0x36, 0x1A)
STITCH = (0xE6, 0xC9, 0x9A)
SLOT_DARK = (0x3A, 0x22, 0x10)
SLOT_LIGHT = (0xB0, 0x7C, 0x4C)
SLOT_INNER = (0x6B, 0x41, 0x22)

FRAME_SIZE, FRAME_BORDER = 16, 4
METAL_DARK = (0x1B, 0x1B, 0x1B)
METAL_LIGHT = (0xE2, 0xE2, 0xE2)
METAL = (0xA8, 0xA8, 0xA8)
METAL_SHADOW = (0x5A, 0x5A, 0x5A)


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


def frame(pixels, outline, open_left=False):
	"""Outline with cut corners and a one pixel bevel, like vanilla's panels. An open left side joins another panel."""
	height, width = len(pixels), len(pixels[0])
	for y in range(height):
		for x in range(width):
			if open_left and x < 2 and y not in (0, 1, height - 2, height - 1):
				continue
			edge_x = x == width - 1 or x == 0 and not open_left
			edge_y = y in (0, height - 1)
			if edge_x and edge_y:
				pixels[y][x] = (0, 0, 0, 0)
			elif edge_x or edge_y or (x in (1, width - 2) and not (open_left and x == 1) and y in (1, height - 2)):
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


def tab(highlighted):
	"""Opens the backpack: a tab stuck to the right edge of the inventory, so its left side is open."""
	pixels = leather(TAB_SIZE, TAB_SIZE, LEATHER_LIGHT if highlighted else LEATHER, seed=20)
	frame(pixels, STITCH if highlighted else OUTLINE, open_left=True)
	return pixels


def close_button(highlighted):
	"""Small leather button with a stitched cross, in the corner of the backpack panel."""
	pixels = leather(CLOSE_SIZE, CLOSE_SIZE, LEATHER_LIGHT if highlighted else LEATHER, seed=11)
	frame(pixels, STITCH if highlighted else OUTLINE)
	for i in range(3, CLOSE_SIZE - 3):
		for x, y in ((i, i), (CLOSE_SIZE - 1 - i, i)):
			pixels[y][x] = STITCH + (255,)
			if y + 1 < CLOSE_SIZE - 2 and pixels[y + 1][x][:3] != STITCH:
				pixels[y + 1][x] = SHADOW + (255,)
	return pixels


def minimap_frame():
	"""Rounded metal bezel: dark outline, lit top-left edge, metal band, dark inner line, empty center."""
	size, last = FRAME_SIZE, FRAME_SIZE - 1
	pixels = [[(0, 0, 0, 0)] * size for _ in range(size)]
	for y in range(size):
		for x in range(size):
			ring = min(x, y, last - x, last - y)
			corner = min(x, last - x) + min(y, last - y)
			if ring >= FRAME_BORDER or corner < 1:
				continue
			if ring == 0 or corner == 1:
				color = METAL_DARK
			elif ring == 1:
				color = METAL_LIGHT if x <= y and x + y < last or y < x and x + y < last else METAL_SHADOW
			elif ring == 2:
				color = METAL
			else:
				color = METAL_DARK
			pixels[y][x] = color + (255,)
	return pixels


if __name__ == "__main__":
	write_png(SPRITES / "backpack_panel.png", panel())
	write_png(SPRITES / "backpack_button.png", tab(False))
	write_png(SPRITES / "backpack_button_highlighted.png", tab(True))
	write_png(SPRITES / "backpack_close.png", close_button(False))
	write_png(SPRITES / "backpack_close_highlighted.png", close_button(True))
	write_png(GUI_SPRITES / "hud/minimap_frame.png", minimap_frame())
