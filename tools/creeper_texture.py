#!/usr/bin/env python3
"""Draws the creeper's glow, added over its skin by CreeperLook as spiders' eyes are. The skin itself
(textures/entity/creeper/creeper.png) is retouched by hand in src/textures/world.png: this script never writes it.

- creeper_heart.png: the hollow of the ribcage on its chest, glowing faintly all the time, slowly breathing;
- creeper_fuse.png: lit while the creeper swells, more and more: the ribcage's hollow, hottest in its middle, the
  cracks of its sides and back with a halo along them, and the inside of its mouth.

Both follow the skin's layout, vanilla's boxes on a 64 × 32 texture: the head (0, 0), 8 × 8 × 8; the body (16, 16),
8 wide, 12 high and 4 deep; each laid out the game's way, top and bottom faces then the sides around from the right.

Usage: python3 tools/creeper_texture.py, then open the PNGs in src/textures/world.psd (tools/atlas_psd.py)
"""

import pathlib

from pngio import write_png

FOLDER = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/minecraft/textures/entity/creeper"

CLEAR = (0, 0, 0, 0)
EMBER_CORE = (255, 226, 140, 255)
EMBER = (250, 132, 40, 255)
EMBER_HALO = (110, 36, 8, 255)
# The heart's glow, its middle warmer; CreeperLook dims it further
HEART_CORE = (255, 150, 60, 255)
HEART = (170, 70, 20, 255)

# (u, v, width, height, depth)
HEAD = (0, 0, 8, 8, 8)
BODY = (16, 16, 8, 12, 4)

# The ribcage's hollow, on the body's front from its 5th row: around its middle (o), the rest (.); the ribs and the
# skin around them (-) stay dark
CAGE = [
	"---..---",
	"-..oo..-",
	"---..---",
	"-.oooo.-",
	"---..---",
	"-..oo..-",
	"--....--",
]
CAGE_TOP = 4

# Cracks, on the strip of a box's sides (x around from the right side's edge, y down from its top): where each
# starts, and the way it goes at each step, (dx, dy)
CRACKS = [
	(BODY, 1, 5, [(0, 1), (1, 1), (0, 1)]),
	(BODY, 15, 4, [(0, 1), (-1, 1), (0, 1), (0, 1), (1, 1)]),
	(BODY, 20, 5, [(0, 1), (1, 1), (0, 1)]),
	(HEAD, 27, 4, [(0, 1), (1, 1)]),
]

# The inside of the mouth, on the head's front at (8, 8)
MOUTH = [(3, 5), (4, 5), (3, 6), (4, 6)]


def draw():
	fuse = [[CLEAR] * 64 for _ in range(32)]
	heart = [[CLEAR] * 64 for _ in range(32)]

	u, v, w, h, d = BODY
	for y, row in enumerate(CAGE):
		for x, cell in enumerate(row):
			px, py = u + d + x, v + d + CAGE_TOP + y
			if cell == "o":
				fuse[py][px], heart[py][px] = EMBER_CORE, HEART_CORE
			elif cell == ".":
				fuse[py][px], heart[py][px] = EMBER, HEART

	for part, x, y, steps in CRACKS:
		u, v, w, h, d = part
		around = 2 * (w + d)
		path = [(x, y)]
		for dx, dy in steps:
			x, y = (x + dx) % around, y + dy
			if y >= h:
				break
			path.append((x, y))
		for i, (x, y) in enumerate(path):
			fuse[v + d + y][u + x] = EMBER if i in (0, len(path) - 1) else EMBER_CORE
		for x, y in path:
			for nx in ((x - 1) % around, (x + 1) % around):
				if (nx, y) not in path:
					fuse[v + d + y][u + nx] = EMBER_HALO

	for x, y in MOUTH:
		fuse[8 + y][8 + x] = EMBER
	return fuse, heart


if __name__ == "__main__":
	FOLDER.mkdir(parents=True, exist_ok=True)
	fuse_pixels, heart_pixels = draw()
	write_png(FOLDER / "creeper_fuse.png", fuse_pixels)
	write_png(FOLDER / "creeper_heart.png", heart_pixels)
	print("wrote creeper_fuse.png and creeper_heart.png")
