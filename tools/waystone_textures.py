"""Draws the cube floating on top of the waystone (textures/block/waystone_crystal): the End crystal's core, its symbols
replaced by the letters of the word PORTAL in the enchanting table's alphabet, same reds, same glow. The stone's textures are edited by hand in textures/world.png.

Usage:
    python3 tools/waystone_textures.py <client jar> && python3 tools/world_atlas.py
"""

import pathlib
import sys
import zipfile

from pngio import read_png, write_png

TARGET = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/cassonade/textures/block"
CRYSTAL = "assets/minecraft/textures/entity/end_crystal/end_crystal.png"
FONT = "assets/minecraft/textures/font/ascii_sga.png"
# The core's box in the End crystal's texture
CORE_LEFT, CORE_WIDTH, CORE_HEIGHT = 64, 64, 32
# Its symbols, and the glow around them
RED = (228, 34, 129)
RED_SHADE = (198, 17, 124)
GLOW = (247, 106, 210)
SYMBOL = {RED, RED_SHADE, GLOW, (255, 255, 255)}
LILACS = [(227, 129, 255), (216, 119, 255), (213, 111, 255), (209, 97, 255)]
GLYPH = 8
FACE = 16
# Where each face of the box sits: its top and bottom, then its four sides side by side
FACES = {"P": (16, 0), "O": (32, 0), "R": (0, 16), "T": (16, 16), "A": (32, 16), "L": (48, 16)}


def glyph(font, letter):
	"""A letter of the font at its own size, centered in a face: the set of (x, y)."""
	index = ord(letter) - ord("A") + 1
	row, column = 4 + index // 16, index % 16
	dots = {(x, y) for y in range(GLYPH) for x in range(GLYPH) if font[row * GLYPH + y][column * GLYPH + x][3]}
	left, right = min(x for x, _ in dots), max(x for x, _ in dots)
	top, bottom = min(y for _, y in dots), max(y for _, y in dots)
	dx, dy = (FACE - (right - left + 1)) // 2 - left, (FACE - (bottom - top + 1)) // 2 - top
	return {(x + dx, y + dy) for x, y in dots}


def core(crystal, font):
	pixels = [list(row[CORE_LEFT:CORE_LEFT + CORE_WIDTH]) for row in crystal[:CORE_HEIGHT]]
	# The symbols wiped: the lilac of the face's grain, in a pattern as loose as the rest
	for y, row in enumerate(pixels):
		for x, pixel in enumerate(row):
			if pixel[3] and pixel[:3] in SYMBOL:
				row[x] = LILACS[(x * 3 + y * 5) % len(LILACS)] + (255,)
	for letter, (left, top) in FACES.items():
		dots = glyph(font, letter)
		for x, y in dots:
			for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
				nx, ny = x + dx, y + dy
				if (nx, ny) not in dots and 0 <= nx < FACE and 0 <= ny < FACE:
					pixels[top + ny][left + nx] = GLOW + (255,)
		for x, y in dots:
			pixels[top + y][left + x] = (RED_SHADE if (x + y) % 5 == 0 else RED) + (255,)
	return pixels


def main(jar_path):
	with zipfile.ZipFile(jar_path) as jar:
		font = read_png(jar.read(FONT))
		write_png(TARGET / "waystone_crystal.png", core(read_png(jar.read(CRYSTAL)), font))


if __name__ == "__main__":
	main(sys.argv[1])
