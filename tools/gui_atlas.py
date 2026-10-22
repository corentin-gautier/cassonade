#!/usr/bin/env python3
"""Packs every GUI texture of the mod into one image, textures/gui.png, to edit them all in one place.

The build cuts the image back into the game's files (unpackGuiAtlas in build.gradle), following
textures/gui.json: where each texture sits in the image and where it goes in its file. Textures are
cropped to their visible pixels, so paint inside each one's area; the space between them is ignored.

    python3 tools/gui_atlas.py     packs the PNGs found under src/main/resources, then removes them

PNGs found replace the image's textures of the same path; the image's other textures are kept.

tools/build_gui.py redraws every texture from the palette and packs them, replacing the image.
"""

import json
from pathlib import Path

from pngio import read_png, write_png

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets"
ATLAS = ROOT / "textures/gui.png"
LAYOUT = ROOT / "textures/gui.json"

# Sections of the image, from top to bottom: title, then the paths (under assets/) they hold
SECTIONS = [
	("HUD", ["minecraft/textures/gui/sprites/hud/", "vanillasugar/textures/gui/sprites/hud/"]),
	("FENETRES", ["minecraft/textures/gui/container/"]),
	("INVENTAIRE ET CREATIF", ["minecraft/textures/gui/sprites/container/"]),
	("WIDGETS ET INFOBULLES", ["minecraft/textures/gui/sprites/widget/", "minecraft/textures/gui/sprites/tooltip/"]),
	("LIVRE DE RECETTES", ["minecraft/textures/gui/recipe_book.png", "minecraft/textures/gui/sprites/recipe_book/"]),
	("VANILLA SUGAR", ["vanillasugar/textures/gui/"]),
]
WIDTH = 1024
GAP = 4
TITLE_SCALE = 2
TITLE_COLOR = (0x80, 0x80, 0x80, 255)

# 3x5 letters for the section titles
FONT = {
	"A": "010101111101101", "B": "110101110101110", "C": "011100100100011", "D": "110101101101110",
	"E": "111100110100111", "F": "111100110100100", "G": "011100101101011", "H": "101101111101101",
	"I": "111010010010111", "K": "101101110101101", "L": "100100100100111", "N": "110101101101101",
	"O": "010101101101010", "R": "110101110101101", "S": "011100010001110", "T": "111010010010010",
	"U": "101101101101111", "V": "101101101101010", "W": "101101101111101", "Y": "101101010010010",
	" ": "000000000000000",
}


def crop(pixels):
	"""The box around the visible pixels, (x, y, width, height); empty for a blank texture."""
	rows = [y for y, row in enumerate(pixels) if any(p[3] for p in row)]
	if not rows:
		return 0, 0, 0, 0
	columns = [x for x in range(len(pixels[0])) if any(row[x][3] for row in pixels)]
	return columns[0], rows[0], columns[-1] - columns[0] + 1, rows[-1] - rows[0] + 1


def section(path):
	for index, (_, prefixes) in enumerate(SECTIONS):
		if any(path.startswith(prefix) for prefix in prefixes):
			return index
	raise ValueError(f"{path}: no section of the atlas holds it")


def draw_title(image, text, left, top):
	for i, letter in enumerate(text):
		for j, bit in enumerate(FONT[letter]):
			if bit == "1":
				for dy in range(TITLE_SCALE):
					for dx in range(TITLE_SCALE):
						image[top + (j // 3) * TITLE_SCALE + dy][left + (i * 4 + j % 3) * TITLE_SCALE + dx] = TITLE_COLOR


def unpack():
	"""The textures of the image, by path, at their size in the game's files."""
	if not LAYOUT.exists():
		return {}
	atlas = read_png(ATLAS.read_bytes())
	textures = {}
	for path, texture in json.loads(LAYOUT.read_text()).items():
		ax, ay, w, h = texture["atlas"]
		ox, oy = texture["offset"]
		width, height = texture["size"]
		pixels = [[(0, 0, 0, 0)] * width for _ in range(height)]
		for row in range(h):
			pixels[oy + row][ox:ox + w] = atlas[ay + row][ax:ax + w]
		textures[path] = pixels
	return textures


def pack():
	files = sorted(ASSETS.glob("*/textures/gui/**/*.png"))
	if not files:
		raise SystemExit("No GUI texture to pack under " + str(ASSETS))
	images = unpack()
	for file in files:
		images[file.relative_to(ASSETS).as_posix()] = read_png(file.read_bytes())
	textures = [(section(path), path, pixels, crop(pixels)) for path, pixels in images.items()]
	textures.sort(key=lambda t: (t[0], t[1]))

	# Shelves: textures left to right in path order, a new row when the width is used
	placed, y, current = [], GAP, None
	x = row_height = 0
	for index, path, pixels, box in textures:
		if index != current:
			y += row_height + (GAP * 3 if current is not None else 0)
			placed.append(("title", SECTIONS[index][0], GAP, y))
			y += 5 * TITLE_SCALE + GAP
			x, row_height, current = GAP, 0, index
		width, height = box[2], box[3]
		if x + width > WIDTH - GAP:
			x, y, row_height = GAP, y + row_height + GAP, 0
		placed.append(("texture", path, pixels, box, x, y))
		x += width + GAP
		row_height = max(row_height, height)
	height = y + row_height + GAP

	image = [[(0, 0, 0, 0)] * WIDTH for _ in range(height)]
	layout = {}
	for entry in placed:
		if entry[0] == "title":
			draw_title(image, entry[1], entry[2], entry[3])
			continue
		_, path, pixels, (cx, cy, w, h), ax, ay = entry
		for row in range(h):
			image[ay + row][ax:ax + w] = pixels[cy + row][cx:cx + w]
		layout[path] = {"atlas": [ax, ay, w, h], "offset": [cx, cy], "size": [len(pixels[0]), len(pixels)]}

	ATLAS.parent.mkdir(exist_ok=True)
	write_png(ATLAS, image)
	LAYOUT.write_text(json.dumps(layout, indent=1) + "\n")
	for file in files:
		file.unlink()
	print(f"{len(files)} textures packed into {ATLAS.relative_to(ROOT)}, {len(images)} in all ({WIDTH}x{height})")


if __name__ == "__main__":
	pack()
