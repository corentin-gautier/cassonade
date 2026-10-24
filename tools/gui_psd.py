#!/usr/bin/env python3
"""Writes textures/gui.psd from textures/gui.png: the same image, its pixels sorted into one layer per
family of color, to change a family's tint in a drawing program (Photoshop, Photopea, GIMP,
Affinity...) with a Hue/Saturation adjustment clipped to its layer.

- Bois: the brown wood of the panels, buttons, tabs and frames.
- Fonds sombres: outlines, slots, the insides of fields and boxes.
- Cuir: the backpack, the recipe book's and the journal's covers, their stitches.
- Papier: the pages and everything cream written on them.
- Crème: what lights up when hovered, the glare of the buttons.
- Vert: what is selected, the experience bar.
- Or, Améthyste, Rouge: the banners, the ribbon, the hovered cards.
- Icônes: items and pictures drawn into the textures, which keep their colors.
- Divers: whatever fits none of the above.
- Repères: the pink frames and the names around the textures, locked; ignored by the build.

Once the changes are made, export the image, flattened, as textures/gui.png (PNG, transparency
kept): the build cuts it into the game's files as before. The PSD is drawn from gui.png and is not
read back: run this script again after gui.png has changed elsewhere, which redraws the layers
without the adjustments added to them.

    python3 tools/gui_psd.py
"""

import colorsys
import json
import struct
import zlib
from pathlib import Path

from pngio import read_png

ROOT = Path(__file__).resolve().parent.parent
ATLAS = ROOT / "textures/gui.png"
LAYOUT = ROOT / "textures/gui.json"
PSD = ROOT / "textures/gui.psd"

# Bottom to top
LAYERS = ["Divers", "Fonds sombres", "Bois", "Cuir", "Papier", "Crème", "Vert", "Or", "Améthyste", "Rouge", "Icônes", "Repères"]
GUIDES = LAYERS.index("Repères")
# Textures whose leather goes to its layer
LEATHER = ["backpack_panel", "backpack_strap", "minecraft/textures/gui/recipe_book.png", "recipe_book/tab", "recipe_book/overlay_recipe",
	"quest/journal_cover"]
# Textures that are pictures, kept whole
ICONS = ["weather_", "inventory_tab_map", "inventory_tab_recipe_book", "recipe_book/button", "recipe_book/filter_",
	"recipe_book/furnace_filter_", "enchanting_book/", "heart/", "button_journal", "button_advancements", "container/slot/", "map/fog",
	"storage_"]


def family(pixel, leather):
	"""The layer of a pixel of a texture, from its hue, saturation and brightness."""
	h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in pixel[:3]))
	h *= 360
	reddish = h < 30 or h > 330
	if leather and reddish and (0.2 <= s and v < 0.8 or 0.15 <= s <= 0.4 and v >= 0.8):
		return "Cuir"
	if v < 0.2:
		return "Fonds sombres"
	if 70 <= h <= 170 and s > 0.3:
		return "Vert"
	if 255 <= h <= 300 and s > 0.25:
		return "Améthyste"
	if 33 <= h <= 60 and s > 0.45 and v > 0.4:
		return "Or"
	if reddish and s > 0.65 and v > 0.3:
		return "Rouge"
	if v > 0.7 and s < 0.35 and 25 <= h <= 65:
		return "Papier"
	if v > 0.75 and s < 0.3 and reddish:
		return "Crème"
	if reddish and s < 0.5:
		return "Bois"
	return "Divers"


def layers(image, layout):
	"""Which layer each pixel goes to: the textures' by their colors, the rest is the guides."""
	height, width = len(image), len(image[0])
	owner = [[GUIDES] * width for _ in range(height)]
	for name, entry in layout.items():
		x0, y0, w, h = entry["atlas"]
		leather = any(k in name for k in LEATHER)
		icon = any(k in name for k in ICONS)
		for y in range(y0, y0 + h):
			for x in range(x0, x0 + w):
				owner[y][x] = LAYERS.index("Icônes" if icon else family(image[y][x], leather))
	return owner


def pascal(name):
	data = name.encode("mac_roman", "replace")[:255]
	data = bytes([len(data)]) + data
	return data + b"\0" * (-len(data) % 4)


def block(key, data):
	data += b"\0" * (-len(data) % 4)
	return b"8BIM" + key + struct.pack(">I", len(data)) + data


def layer_record(name, bounds, channel_lengths, locked):
	top, left, bottom, right = bounds
	record = struct.pack(">iiiiH", top, left, bottom, right, 4)
	for channel, length in zip((-1, 0, 1, 2), channel_lengths):
		record += struct.pack(">hI", channel, length)
	record += b"8BIMnorm" + struct.pack(">BBBB", 255, 0, 0, 0)
	extra = struct.pack(">II", 0, 0) + pascal(name)
	extra += block(b"luni", struct.pack(">I", len(name)) + name.encode("utf-16-be"))
	if locked:
		# Transparency, composite and position locked
		extra += block(b"lspf", struct.pack(">I", 0x7))
	return record + struct.pack(">I", len(extra)) + extra


def main():
	image = read_png(ATLAS.read_bytes())
	layout = json.loads(LAYOUT.read_text())
	height, width = len(image), len(image[0])
	owner = layers(image, layout)

	records, data, count = b"", b"", 0
	for index, name in enumerate(LAYERS):
		rows = [y for y in range(height) if index in owner[y]]
		if not rows:
			continue
		columns = [x for x in range(width) if any(owner[y][x] == index for y in rows)]
		top, bottom, left, right = rows[0], rows[-1] + 1, columns[0], columns[-1] + 1
		planes = [bytearray(), bytearray(), bytearray(), bytearray()]
		for y in range(top, bottom):
			for x in range(left, right):
				r, g, b, a = image[y][x] if owner[y][x] == index else (0, 0, 0, 0)
				planes[0].append(a)
				planes[1].append(r)
				planes[2].append(g)
				planes[3].append(b)
		# ZIP without prediction
		compressed = [struct.pack(">H", 2) + zlib.compress(bytes(plane), 9) for plane in planes]
		records += layer_record(name, (top, left, bottom, right), [len(c) for c in compressed], index == GUIDES)
		data += b"".join(compressed)
		count += 1
		print(f"{name}: {sum(owner[y][x] == index for y in rows for x in range(left, right))} pixels")

	# A negative count: the merged image's first extra channel is its transparency
	info = struct.pack(">h", -count) + records + data
	info += b"\0" * (len(info) % 2)
	layer_and_mask = struct.pack(">I", len(info)) + info + struct.pack(">I", 0)

	merged = struct.pack(">H", 0) + b"".join(bytes(row[x][c] for row in image for x in range(width)) for c in range(4))
	psd = (b"8BPS" + struct.pack(">H6xHIIHH", 1, 4, height, width, 8, 3) + struct.pack(">I", 0) + struct.pack(">I", 0)
		+ struct.pack(">I", len(layer_and_mask)) + layer_and_mask + merged)
	PSD.write_bytes(psd)
	print(f"{PSD.relative_to(ROOT)} written, {len(psd) // 1024} KB")


if __name__ == "__main__":
	main()
