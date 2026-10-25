"""Draws the mod's icon: a lump of cassonade, the coarse amber cane sugar, on a wood panel, in the colors
of the mod's interface.

Drawn at 32×32 and scaled up without smoothing, so it stays pixel art. Writes the icon shown in the
mod list (assets/cassonade/icon.png) and the website's copy (site/assets/icon.png).

Usage:
    python3 tools/icon_texture.py
"""

import pathlib

import gui_theme as theme
from pngio import write_png

ROOT = pathlib.Path(__file__).resolve().parent.parent
TARGETS = [ROOT / "src/main/resources/assets/cassonade/icon.png", ROOT / "site/assets/icon.png"]
SIZE = 32
SCALE = 4


def rgb(color, alpha=255):
	color = color.lstrip("#")
	return tuple(int(color[i:i + 2], 16) for i in (0, 2, 4)) + (alpha,)


CLEAR = (0, 0, 0, 0)
OUTLINE = rgb(theme.LEATHER_OUTLINE)
WOOD = rgb(theme.WOOD)
WOOD_LIGHT = rgb(theme.WOOD_LIGHT)
WOOD_DARK = rgb(theme.WOOD_DARK)
SHADOW = rgb("#30201f")
# Amber faces, each with the glint of its crystals and a darker speck of molasses
SUGAR_TOP = rgb("#DDA55A")
SUGAR_TOP_GRAIN = rgb("#F7D394")
SUGAR_TOP_SPECK = rgb("#B97B3A")
SUGAR_LEFT = rgb("#BC7E3B")
SUGAR_LEFT_GRAIN = rgb("#DDA55A")
SUGAR_LEFT_SPECK = rgb("#94592A")
SUGAR_RIGHT = rgb("#93592A")
SUGAR_RIGHT_GRAIN = rgb("#B4773A")
SUGAR_RIGHT_SPECK = rgb("#6E3F1E")
SUGAR_EDGE = rgb("#FBE2AE")
GOLD = rgb(theme.GOLD)
GOLD_LIGHT = rgb(theme.GOLD_LIGHT)

# The cube, seen from above: its top face is a diamond, its two visible sides hang below it
CENTER_X, TOP_Y = 16, 5
HALF_WIDTH, HALF_HEIGHT, DEPTH = 12, 6, 12


def panel(x, y):
	"""The wood tile behind the cube, with the bevel of the mod's windows and rounded corners."""
	corner = min(x, SIZE - 1 - x) + min(y, SIZE - 1 - y)
	edge = min(x, y, SIZE - 1 - x, SIZE - 1 - y)
	if corner < 3:
		return CLEAR
	if edge == 0 or corner == 3:
		return OUTLINE
	if edge == 1 or corner == 4:
		return WOOD_LIGHT if x < SIZE - 2 and y < SIZE - 2 and x + y < SIZE else WOOD_DARK
	# Faint wood grain
	if y % 6 == 3 and (x * 7 + y) % 11 > 2:
		return WOOD_DARK
	return WOOD


def cube_face(x, y):
	"""Which face of the cube covers this pixel: "top", "left", "right" or None."""
	px, py = x + 0.5 - CENTER_X, y + 0.5 - (TOP_Y + HALF_HEIGHT)
	if abs(px) > HALF_WIDTH:
		return None
	# Top face: |px| / w + |py| / h <= 1
	if abs(px) / HALF_WIDTH + abs(py) / HALF_HEIGHT <= 1:
		return "top"
	# Sides: between the top face's lower edge and the same edge moved down by the depth
	lower_edge = HALF_HEIGHT * (1 - abs(px) / HALF_WIDTH)
	if lower_edge < py <= lower_edge + DEPTH:
		return "left" if px < 0 else "right"
	return None


def grain(x, y):
	"""Scattered pixels for the coarse crystals of cassonade: "glint", "speck" or None."""
	n = (x * 37 + y * 61 + x * y * 13) % 17
	return "glint" if n in (0, 7) else "speck" if n == 11 else None


# Loose crystals fallen on the wood, by the cube's foot
CRYSTALS = ((4, 25), (7, 27), (5, 28), (27, 7))


def draw():
	pixels = [[panel(x, y) for x in range(SIZE)] for y in range(SIZE)]
	faces = [[cube_face(x, y) for x in range(SIZE)] for y in range(SIZE)]

	# Drop shadow, down and to the right
	for y in range(SIZE):
		for x in range(SIZE):
			if faces[y][x] is None and pixels[y][x] in (WOOD, WOOD_DARK):
				if 1 <= x - 2 and y - 2 >= 0 and faces[y - 2][x - 2] is not None:
					pixels[y][x] = SHADOW

	colors = {
		"top": (SUGAR_TOP, SUGAR_TOP_GRAIN, SUGAR_TOP_SPECK),
		"left": (SUGAR_LEFT, SUGAR_LEFT_GRAIN, SUGAR_LEFT_SPECK),
		"right": (SUGAR_RIGHT, SUGAR_RIGHT_GRAIN, SUGAR_RIGHT_SPECK),
	}
	for y in range(SIZE):
		for x in range(SIZE):
			face = faces[y][x]
			if face is None:
				continue
			neighbors = [faces[y + dy][x + dx] if 0 <= x + dx < SIZE and 0 <= y + dy < SIZE else None
				for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
			if None in neighbors:
				pixels[y][x] = OUTLINE
			elif face == "top" and ("left" in neighbors or "right" in neighbors):
				# The lit ridge along the top's lower edges
				pixels[y][x] = SUGAR_EDGE
			elif face == "left" and "right" in neighbors:
				# The vertical edge between the sides
				pixels[y][x] = SUGAR_LEFT_GRAIN
			else:
				base, glint, speck = colors[face]
				kind = grain(x, y)
				pixels[y][x] = glint if kind == "glint" else speck if kind == "speck" else base

	for x, y in CRYSTALS:
		pixels[y][x] = SUGAR_TOP
		pixels[y + 1][x] = SUGAR_RIGHT

	# Two gold sparkles
	for cx, cy, big in ((6, 6, True), (26, 26, False)):
		pixels[cy][cx] = GOLD_LIGHT
		for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
			pixels[cy + dy][cx + dx] = GOLD
		if big:
			for dx, dy in ((2, 0), (-2, 0), (0, 2), (0, -2)):
				pixels[cy + dy][cx + dx] = GOLD
	return pixels


def scaled(pixels):
	return [[pixel for pixel in row for _ in range(SCALE)] for row in pixels for _ in range(SCALE)]


if __name__ == "__main__":
	icon = scaled(draw())
	for target in TARGETS:
		write_png(target, icon)
		print("wrote", target.relative_to(ROOT))
