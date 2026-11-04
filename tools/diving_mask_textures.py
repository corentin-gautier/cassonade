"""Draws the diving mask from the game's leather and glass:

- the item (textures/item/diving_mask.png): a leather frame around a glass pane, its strap's ends on each side, a
  nose pocket under the glass;
- the mask worn (textures/entity/equipment/humanoid/diving_mask.png, a helmet's layout): the frame and glass over
  the eyes, wrapping a pixel round the sides, and the strap all around the head.

Colors come from the game's leather item and glass block. The website shows the same icon (promo/site/assets/items).

Usage:
    python3 tools/diving_mask_textures.py [path to the Minecraft jar]
"""

import pathlib
import sys
import zipfile

from build_gui import loom_jar
from pngio import read_png, write_png

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/cassonade/textures"
SITE = ROOT / "promo/site/assets/items"
CLEAR = (0, 0, 0, 0)

# The item, 16 by 16: . nothing, f frame, g glass, s strap
ITEM = [
	"................",
	"................",
	"................",
	"....ffffffff....",
	"..ffffffffffff..",
	"..ffggggggggff..",
	"..fggggggggggf..",
	"ssfggggggggggfss",
	"ssfggggggggggfss",
	"..fggggggggggf..",
	"..ffgggffgggff..",
	"..ffffffffffff..",
	"......ffff......",
	"................",
	"................",
	"................",
]


def palette(pixels):
	"""The image's colors, darkest first."""
	colors = {px for row in pixels for px in row if px[3]}
	return sorted(colors, key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])


def item(leather, glass):
	rows = ITEM
	pixels = [[CLEAR] * 16 for _ in range(16)]

	def at(x, y):
		return rows[y][x] if 0 <= x < 16 and 0 <= y < 16 else "."

	for y in range(16):
		for x in range(16):
			kind = at(x, y)
			if kind == "f":
				outside = any(at(x + dx, y + dy) == "." for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
				lip = any(at(x + dx, y + dy) == "g" for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
				if outside:
					pixels[y][x] = leather[0]
				elif lip:
					pixels[y][x] = leather[1]
				else:
					pixels[y][x] = leather[5] if y <= 4 else leather[3] if y <= 9 else leather[2]
			elif kind == "s":
				pixels[y][x] = leather[3] if y == 7 else leather[1]
			elif kind == "g":
				# Lit from the top left: a glint across it, the far corner darker
				diagonal = (x - 3) + (y - 5)
				if diagonal in (2, 3) and y <= 7:
					pixels[y][x] = glass[3]
				elif diagonal <= 5:
					pixels[y][x] = glass[2]
				elif diagonal <= 10:
					pixels[y][x] = glass[1]
				else:
					pixels[y][x] = glass[0]
	return pixels


def worn(leather, glass):
	"""A helmet's layout: the head's faces around (0, 8), its front at (8, 8), each 8 by 8."""
	pixels = [[CLEAR] * 64 for _ in range(32)]
	front_x, face_y = 8, 8
	# The frame over the eyes, rows 1 to 5 of the face, the glass inside it
	for row in range(1, 6):
		for col in range(8):
			glassy = 2 <= row <= 4 and 1 <= col <= 6
			if glassy:
				diagonal = col + row
				color = glass[3] if diagonal == 4 else glass[2] if diagonal <= 6 else glass[1] if diagonal <= 9 else glass[0]
			else:
				color = leather[4] if row == 1 else leather[2] if row == 5 else leather[3]
			pixels[face_y + row][front_x + col] = color
	# The nose pocket under the glass
	pixels[face_y + 6][front_x + 3] = leather[2]
	pixels[face_y + 6][front_x + 4] = leather[2]
	# The frame wraps a pixel round each side: the right side's last column, the left side's first
	for row in range(1, 6):
		pixels[face_y + row][7] = leather[2]
		pixels[face_y + row][16] = leather[2]
	# The strap, at the eyes' height, round the sides and the back
	for x in list(range(0, 7)) + list(range(17, 32)):
		pixels[face_y + 3][x] = leather[3]
		pixels[face_y + 4][x] = leather[1]
	return pixels


def main(jar_path):
	with zipfile.ZipFile(jar_path) as jar:
		leather = palette(read_png(jar.read("assets/minecraft/textures/item/leather.png")))
		glass = palette(read_png(jar.read("assets/minecraft/textures/block/glass.png")))
	icon = item(leather, glass)
	(ASSETS / "item").mkdir(parents=True, exist_ok=True)
	write_png(ASSETS / "item/diving_mask.png", icon)
	write_png(SITE / "diving_mask.png", icon)
	(ASSETS / "entity/equipment/humanoid").mkdir(parents=True, exist_ok=True)
	write_png(ASSETS / "entity/equipment/humanoid/diving_mask.png", worn(leather, glass))


if __name__ == "__main__":
	main(sys.argv[1] if len(sys.argv) > 1 else loom_jar())
