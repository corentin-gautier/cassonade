#!/usr/bin/env python3
"""Draws the filter hopper's textures from the vanilla hopper's: an iron sieve across the bowl, and a
row of rivets around the sides so it can be told apart from a hopper from any angle.

    python3 tools/filter_hopper_textures.py [path/to/minecraft-26.3.jar]

Without a jar, the client jar Loom downloaded for the build is used (run ./gradlew build once first).
"""

import sys
import zipfile
from pathlib import Path

from build_gui import loom_jar
from pngio import read_png, write_png

TEXTURES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/vanillasugar/textures"
IRON = (150, 150, 155, 255)
IRON_LIGHT = (185, 185, 190, 255)
IRON_DARK = (100, 100, 105, 255)
# The bowl shows the inside texture between the rim, from 2 to 13: holes of 2 pixels between wires
WIRES = (4, 7, 10, 13)


def sieve(inside):
	pixels = [list(row) for row in inside]
	for y in range(2, 14):
		for x in range(2, 14):
			if x in WIRES and y in WIRES:
				pixels[y][x] = IRON_LIGHT
			elif x in WIRES or y in WIRES:
				pixels[y][x] = IRON
	return pixels


def rivets(outside):
	pixels = [list(row) for row in outside]
	# The dark band across the middle of the sides
	for x in range(1, 16, 3):
		pixels[6][x] = IRON_LIGHT
		pixels[7][x] = IRON_DARK
	return pixels


def item_sieve(item):
	pixels = [list(row) for row in item]
	# The bowl seen from the side, two rows deep
	for x in range(5, 11):
		pixels[4][x] = IRON if x % 2 else IRON_DARK
		pixels[5][x] = IRON_DARK if x % 2 else IRON
	return pixels


def main(jar):
	with zipfile.ZipFile(jar) as archive:
		def vanilla(path):
			return read_png(archive.read("assets/minecraft/textures/" + path))

		write_png(TEXTURES / "block/filter_hopper_sieve.png", sieve(vanilla("block/hopper_inside.png")))
		write_png(TEXTURES / "block/filter_hopper_outside.png", rivets(vanilla("block/hopper_outside.png")))
		write_png(TEXTURES / "item/filter_hopper.png", item_sieve(vanilla("item/hopper.png")))


if __name__ == "__main__":
	main(sys.argv[1] if len(sys.argv) > 1 else str(loom_jar()))
