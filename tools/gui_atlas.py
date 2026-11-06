"""The interface atlas, src/textures/gui.png: its sections, its labels, and its textures read back.

The build cuts the image into the game's files (unpackTextureAtlases in build.gradle), following
src/textures/gui.json: where each texture sits in the image and where it goes in its file. Each texture is
cropped to its visible pixels, in a bright pink frame with its name above: paint inside the frame. The
frames, names and the space between are ignored.

The image is written from src/textures/gui.psd by tools/atlas_psd.py, never laid out again: a texture keeps
its place, a new one goes where its layer is.
"""

import json
from pathlib import Path

from pngio import read_png

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets"
ATLAS = ROOT / "src/textures/gui.png"
LAYOUT = ROOT / "src/textures/gui.json"

# Sections of the image, from top to bottom: title, then the paths (under assets/) they hold
SECTIONS = [
	("HUD", ["minecraft/textures/gui/sprites/hud/", "cassonade/textures/gui/sprites/hud/"]),
	("FENETRES", ["minecraft/textures/gui/container/"]),
	("INVENTAIRE ET CREATIF", ["minecraft/textures/gui/sprites/container/"]),
	("WIDGETS ET INFOBULLES", ["minecraft/textures/gui/sprites/widget/", "minecraft/textures/gui/sprites/tooltip/",
		"minecraft/textures/gui/sprites/toast/"]),
	("LIVRE DE RECETTES", ["minecraft/textures/gui/recipe_book.png", "minecraft/textures/gui/sprites/recipe_book/"]),
	("CASSONADE", ["cassonade/textures/gui/"]),
]
# 3x5 letters for the section titles and the textures' labels
FONT = {
	"A": "010101111101101", "B": "110101110101110", "C": "011100100100011", "D": "110101101101110",
	"E": "111100110100111", "F": "111100110100100", "G": "011100101101011", "H": "101101111101101",
	"I": "111010010010111", "J": "001001001101010", "K": "101101110101101", "L": "100100100100111",
	"M": "101111111101101", "N": "110101101101101", "O": "010101101101010", "P": "110101110100100",
	"Q": "010101101110011", "R": "110101110101101", "S": "011100010001110", "T": "111010010010010",
	"U": "101101101101111", "V": "101101101101010", "W": "101101111111101", "X": "101101010101101",
	"Y": "101101010010010", "Z": "111001010100111",
	"0": "111101101101111", "1": "010110010010111", "2": "110001010100111", "3": "110001010001110",
	"4": "101101111001001", "5": "111100110001110", "6": "011100110101010", "7": "111001010010010",
	"8": "010101010101010", "9": "010101011001110",
	"_": "000000000000111", "-": "000000111000000", "/": "001001010100100", ".": "000000000000010",
	" ": "000000000000000",
}
LABEL_COLOR = (0, 0, 0, 255)
FRAME_COLOR = (0xFF, 0x00, 0xC8, 255)
LABEL_HEIGHT = 5
LABEL_GAP = 2


def section(path, sections=SECTIONS):
	for index, (_, prefixes) in enumerate(sections):
		if any(path.startswith(prefix) for prefix in prefixes):
			return index
	raise ValueError(f"{path}: no section of the atlas holds it")


def draw_text(image, text, left, top, scale, color):
	for i, letter in enumerate(text.upper()):
		for j, bit in enumerate(FONT.get(letter, FONT[" "])):
			if bit == "1":
				for dy in range(scale):
					for dx in range(scale):
						image[top + (j // 3) * scale + dy][left + (i * 4 + j % 3) * scale + dx] = color


def text_width(text):
	return len(text) * 4 - 1


def label(path):
	"""The texture's name in the atlas: its path under textures/gui, without "sprites/" and the extension."""
	return path.split("/textures/gui/", 1)[1].removeprefix("sprites/").removesuffix(".png")


def unpack(atlas_path=ATLAS, layout_path=LAYOUT):
	"""The textures of the image, by path, at their size in the game's files."""
	if not layout_path.exists():
		return {}
	atlas = read_png(atlas_path.read_bytes())
	textures = {}
	for path, texture in json.loads(layout_path.read_text()).items():
		ax, ay, w, h = texture["atlas"]
		ox, oy = texture["offset"]
		width, height = texture["size"]
		pixels = [[(0, 0, 0, 0)] * width for _ in range(height)]
		for row in range(h):
			pixels[oy + row][ox:ox + w] = atlas[ay + row][ax:ax + w]
		textures[path] = pixels
	return textures
