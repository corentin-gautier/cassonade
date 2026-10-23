#!/usr/bin/env python3
"""Generates the mod's GUI sprites.

- Leather backpack panel and the recipe book's ribbon.
- Weather icons of the clock HUD, and their small version for the forecast.
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
ICON_OUTLINE = rgb(theme.OUTLINE)
WEATHER_COLORS = {
	"Y": rgb(theme.SUN), "y": rgb(theme.SUN_RAYS), "M": rgb(theme.MOON), "m": rgb(theme.MOON_SHADE),
	"C": rgb(theme.CLOUD), "c": rgb(theme.CLOUD_SHADE), "D": rgb(theme.STORM_CLOUD), "d": rgb(theme.STORM_CLOUD_SHADE),
	"R": rgb(theme.RAIN), "S": rgb(theme.SNOW), "B": rgb(theme.BOLT),
}
# Weather icons, 11 x 11, outlined by weather_icon(): the letters are WEATHER_COLORS
WEATHER_ICONS = {
	"clear_day": [
		".....y.....",
		".y...y...y.",
		"..y.....y..",
		"....YYY....",
		"...YYYYY...",
		"yy.YYYYY.yy",
		"...YYYYY...",
		"....YYY....",
		"..y.....y..",
		".y...y...y.",
		".....y.....",
	],
	"clear_night": [
		"...MMMM....",
		"..MMMm.....",
		".MMMm......",
		"MMMm.......",
		"MMMm.......",
		"MMMm.......",
		"MMMm.......",
		"MMMMm......",
		".MMMMm...m.",
		"..MMMMMMMM.",
		"...MMMMMM..",
	],
	"cloudy": [
		"...........",
		"...........",
		"....CCC....",
		"...CCCCC...",
		".CCCCCCCCC.",
		"CCCCCCCCCCC",
		"CCCCCCCCCCC",
		".ccccccccc.",
		"...........",
		"...........",
		"...........",
	],
	"rain": [
		"....CCC....",
		"...CCCCC...",
		".CCCCCCCCC.",
		"CCCCCCCCCCC",
		"CCCCCCCCCCC",
		".ccccccccc.",
		"...........",
		".R...R...R.",
		".R...R...R.",
		"R...R...R..",
		"...........",
	],
	"snow": [
		"....CCC....",
		"...CCCCC...",
		".CCCCCCCCC.",
		"CCCCCCCCCCC",
		"CCCCCCCCCCC",
		".ccccccccc.",
		"...........",
		".S...S...S.",
		"...S...S...",
		".S...S...S.",
		"...........",
	],
	"thunder": [
		"....DDD....",
		"...DDDDD...",
		".DDDDDDDDD.",
		"DDDDDDDDDDD",
		"DDDDDDDDDDD",
		".ddddBBddd.",
		".....BB....",
		"....BBBB...",
		".....BB....",
		"....BB.....",
		"....B......",
	],
}



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


# Same icons, 7 x 7, for the forecast under the current weather
SMALL_WEATHER_ICONS = {
	"clear_day": ["...y...", ".y...y.", "..YYY..", "y.YYY.y", "..YYY..", ".y...y.", "...y..."],
	"clear_night": ["..MMM..", ".MMm...", "MMm....", "MMm....", "MMm....", ".MMm.m.", "..MMMM."],
	"cloudy": [".......", "..CCC..", ".CCCCC.", "CCCCCCC", ".ccccc.", ".......", "......."],
	"rain": ["..CCC..", ".CCCCC.", "CCCCCCC", ".ccccc.", ".R.R.R.", ".R.R.R.", "......."],
	"snow": ["..CCC..", ".CCCCC.", "CCCCCCC", ".ccccc.", ".......", ".S.S.S.", "..S.S.."],
	"thunder": ["..DDD..", ".DDDDD.", "DDDDDDD", ".ddBBd.", "..BB...", "...B...", "..B...."],
}


def weather_icon(art):
	"""The art in a 1 pixel dark outline, so the icon reads on any background."""
	size = len(art) + 2
	pixels = [[(0, 0, 0, 0)] * size for _ in range(size)]
	for y, row in enumerate(art):
		for x, letter in enumerate(row):
			if letter != ".":
				pixels[y + 1][x + 1] = WEATHER_COLORS[letter] + (255,)
	for y in range(size):
		for x in range(size):
			if pixels[y][x][3] == 0 and any(0 <= y + dy < size and 0 <= x + dx < size and pixels[y + dy][x + dx][3] == 255
					and pixels[y + dy][x + dx][:3] != ICON_OUTLINE for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1))):
				pixels[y][x] = ICON_OUTLINE + (160,)
	return pixels


def weather_icons():
	for name, art in WEATHER_ICONS.items():
		write_png(GUI_SPRITES / "hud" / f"weather_{name}.png", weather_icon(art))
	for name, art in SMALL_WEATHER_ICONS.items():
		write_png(GUI_SPRITES / "hud" / f"weather_{name}_small.png", weather_icon(art))


def main():
	write_png(SPRITES / "backpack_panel.png", panel())
	write_png(SPRITES / "recipe_book_ribbon.png", ribbon(False))
	write_png(SPRITES / "recipe_book_ribbon_highlighted.png", ribbon(True))
	weather_icons()


if __name__ == "__main__":
	main()
