#!/usr/bin/env python3
"""Draws the tavern keeper, the villager working at the brewing stand in place of the cleric: its profession's overlay
(textures/entity/villager/profession/cleric.png, and the zombie villager's, which vanilla draws the same) is a green
waistcoat buttoned in brass over the robe, and a cream half apron tied at the waist, drawn pixel by pixel. The
leatherworker already wears a leather apron and the butcher a white bib one, so the waistcoat sets him apart.

The overlay covers the villager's jacket, the box at (0, 38) of the 64 × 64 texture, 8 wide, 20 high and 6 deep:
its top face at (6, 38), and its sides around from (0, 44): right (0), front (6), left (14), back (20).

Usage: python3 tools/tavern_keeper_texture.py, then open the PNG in src/textures/world.psd (tools/atlas_psd.py)
"""

import pathlib

from pngio import write_png

ASSETS = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/minecraft/textures/entity"
TARGETS = [ASSETS / "villager/profession/cleric.png", ASSETS / "zombie_villager/profession/cleric.png"]

CLEAR = (0, 0, 0, 0)
WAISTCOAT = [(70, 92, 56, 255), (62, 82, 50, 255), (80, 104, 64, 255)]
WAISTCOAT_EDGE = (44, 58, 36, 255)
BRASS = (204, 168, 72, 255)
TIE = (82, 58, 38, 255)
APRON = [(226, 214, 182, 255), (214, 200, 166, 255), (232, 222, 194, 255)]
APRON_HEM = (184, 168, 134, 255)

TOP, SIDES = 38, 44
CHEST = range(SIDES, SIDES + 9)  # the waistcoat's rows
WAIST = SIDES + 9  # the apron's tie
APRON_ROWS = range(WAIST + 1, WAIST + 9)


def shade(colors, x, y):
	"""One of the cloth's tones, fixed for each pixel so the weave doesn't change from one run to the next."""
	n = (x * 73 + y * 151 + x * y * 7) % 11
	return colors[1] if n < 3 else colors[2] if n > 8 else colors[0]


def draw():
	pixels = [[CLEAR] * 64 for _ in range(64)]

	def put(x, y, color):
		pixels[y][x] = color

	# Shoulders, on the top face: the waistcoat's two sides, from front to back
	for y in range(TOP, SIDES):
		for x in (6, 7, 8, 11, 12, 13):
			put(x, y, shade(WAISTCOAT, x, y))
	# Chest, all around: open at the front in a narrowing V, closed with three brass buttons
	for y in CHEST:
		gap = range(8, 12) if y < SIDES + 2 else range(9, 11) if y < SIDES + 6 else range(0)
		for x in range(28):
			if 6 <= x <= 13 and x in gap:
				continue
			put(x, y, shade(WAISTCOAT, x, y))
		if y == CHEST[-1]:
			for x in range(28):
				put(x, y, WAISTCOAT_EDGE)
	for y in (SIDES + 3, SIDES + 5, SIDES + 7):
		put(11, y, BRASS)
	# The apron's tie, all around the waist, and its ends hanging at the back
	for x in range(28):
		put(x, WAIST, TIE)
	put(23, WAIST + 1, TIE)
	put(24, WAIST + 1, TIE)
	put(23, WAIST + 2, TIE)
	# The half apron, at the front, down to the knees
	for y in APRON_ROWS:
		for x in range(7, 13):
			put(x, y, APRON_HEM if y == APRON_ROWS[-1] or x in (7, 12) else shade(APRON, x, y))
	return pixels


if __name__ == "__main__":
	texture = draw()
	for target in TARGETS:
		target.parent.mkdir(parents=True, exist_ok=True)
		write_png(target, texture)
