#!/usr/bin/env python3
"""Generates the sheets of the cartography table's window, a game pixel per pixel, in the paper of the
recipe book: cleanly cut, framed like a surveyed map and folded in three. The game draws the map inside
the frame and its unexplored parts are transparent, so only faint creases go there. Keep the frames on
what CartographyTableScreen draws: 58 pixels from 4 for a map, 44 from 3 for a copy, 29 from 18 scaled.

    python3 tools/cartography_textures.py
    python3 tools/gui_atlas.py
"""

from pathlib import Path

import gui_theme as theme
from gui_theme import rgb
from pngio import write_png

SPRITES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/minecraft/textures/gui/sprites/container/cartography_table"

OUTLINE = rgb(theme.OUTLINE)
PAPER = rgb(theme.PAPER)
PAPER_SHADOW = rgb(theme.PAPER_SHADOW)
PAGE_EDGE = rgb(theme.PAGE_EDGE)
INK = rgb(theme.INK)
SIZE = 66
# The game stretches a copy's sprite to 50 by 66: only its top is drawn, the rest stays empty
COPY_SIZE, COPY_HEIGHT = 50, 66
TICK_SPACING = 8


def noise(x, y):
	h = (x * 374761393 + y * 668265263) & 0xFFFFFFFF
	h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
	return ((h ^ (h >> 16)) & 0xFF) / 255 - 0.5


def fibers(x, y):
	return tuple(max(0, min(255, c + round(noise(x, y) * 4))) for c in PAPER)


def blend(color, other, amount):
	return tuple(round(a + (b - a) * amount) for a, b in zip(color, other))


def sheet(size, height=None):
	"""A leaf with its corners cut, lit from the top left."""
	pixels = [[(0, 0, 0, 0)] * size for _ in range(height or size)]
	last = size - 1
	for y in range(size):
		for x in range(size):
			corner = min(x, last - x) + min(y, last - y)
			if corner == 0:
				continue
			if corner == 1 or min(x, y, last - x, last - y) == 0:
				color = OUTLINE
			elif x == last - 1 or y == last - 1:
				color = PAGE_EDGE
			elif corner == 2:
				color = PAGE_EDGE
			else:
				color = fibers(x, y)
			pixels[y][x] = color + (255,)
	return pixels


def box(pixels, inset, color, dash=None):
	"""A square line inset from the sheet's edges, dashed if asked, starting from each corner."""
	last = len(pixels[0]) - 1 - inset
	for i in range(inset, last + 1):
		if dash and (i - inset) % dash >= dash // 2 + 1 and last - i >= dash // 2 + 1:
			continue
		for x, y in ((i, inset), (i, last), (inset, i), (last, i)):
			pixels[y][x] = color + (255,)


def neatline(pixels, inset):
	box(pixels, inset, blend(PAGE_EDGE, INK, 0.35))


def folds(pixels, inset):
	"""Where the sheet was folded in three: a faint crease, a paler line lit beside it."""
	size = len(pixels[0])
	inner = size - 2 * inset
	for fold in (inset + inner // 3, inset + inner - inner // 3 - 1):
		for i in range(inset + 1, size - inset - 1):
			for x, y, light in ((fold, i, False), (fold + 1, i, True), (i, fold, False), (i, fold + 1, True)):
				base = pixels[y][x][:3]
				pixels[y][x] = (blend(base, (255, 255, 255), 0.25) if light else blend(base, PAGE_EDGE, 0.3)) + (255,)


def ticks(pixels, inset):
	"""The surveyor's graduation in the margin, between the edge and the frame."""
	size = len(pixels[0])
	color = blend(PAGE_EDGE, INK, 0.35) + (255,)
	for i in range(inset + TICK_SPACING, size - inset - 1, TICK_SPACING):
		for x, y in ((i, inset - 1), (i, size - inset), (inset - 1, i), (size - inset, i)):
			pixels[y][x] = color


def map_sheet():
	pixels = sheet(SIZE)
	folds(pixels, 3)
	neatline(pixels, 3)
	ticks(pixels, 3)
	return pixels


def copy_sheet():
	"""Smaller and stacked: only the frame fits around the map."""
	pixels = sheet(COPY_SIZE, COPY_HEIGHT)
	folds(pixels, 2)
	neatline(pixels, 2)
	return pixels


def scaled_sheet():
	"""The map shrunk in the frame of the sheet it becomes, arrows pointing to the corners it will reach."""
	pixels = sheet(SIZE)
	box(pixels, 3, INK, dash=4)
	box(pixels, 17, INK)
	arrow = blend(PAGE_EDGE, INK, 0.35) + (255,)
	last = SIZE - 1
	for i in range(6, 15):
		for x in (i, last - i):
			for y in (i, last - i):
				pixels[y][x] = arrow
	for reach in (1, 2):
		for x, step_x in ((6, 1), (last - 6, -1)):
			for y, step_y in ((6, 1), (last - 6, -1)):
				pixels[y][x + reach * step_x] = arrow
				pixels[y + reach * step_y][x] = arrow
	return pixels


def main():
	SPRITES.mkdir(parents=True, exist_ok=True)
	write_png(SPRITES / "map.png", map_sheet())
	write_png(SPRITES / "duplicated_map.png", copy_sheet())
	write_png(SPRITES / "scaled_map.png", scaled_sheet())


if __name__ == "__main__":
	main()
