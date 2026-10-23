"""Draws the world map item (textures/item/world_map.png): a filled map's terrain framed by sticks,
like its recipe, an empty map surrounded by eight sticks. Colors come from the game's own textures.

Usage:
    python3 tools/world_map_texture.py ~/Library/Application\\ Support/minecraft/versions/26.3/26.3.jar
"""

import pathlib
import sys
import zipfile

from pngio import read_png, write_png

ITEM = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/vanillasugar/textures/item"


def world_map(filled_map, stick):
	outline, light, dark = stick[2][13], stick[3][13], stick[5][11]
	size = 16
	pixels = [[(0, 0, 0, 0)] * size for _ in range(size)]
	for y in range(size):
		for x in range(size):
			edge = min(x, y, size - 1 - x, size - 1 - y)
			if edge == 0:
				pixels[y][x] = outline
			elif edge == 1:
				# Lit from the top left, like the sticks' own shading
				pixels[y][x] = light if y == 1 or x == 1 else dark
			else:
				pixels[y][x] = filled_map[y][x]
	return pixels


def main(jar_path):
	with zipfile.ZipFile(jar_path) as jar:
		filled_map = read_png(jar.read("assets/minecraft/textures/item/filled_map.png"))
		stick = read_png(jar.read("assets/minecraft/textures/item/stick.png"))
	write_png(ITEM / "world_map.png", world_map(filled_map, stick))


if __name__ == "__main__":
	main(sys.argv[1])
