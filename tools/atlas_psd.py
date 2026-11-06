#!/usr/bin/env python3
"""The two texture atlases, src/textures/gui.png and world.png, are written from Photoshop files kept next to
them, gui.psd and world.psd. The PSDs stay on the author's machine (git ignores them): the PNGs and their .json
are what the repository and the build use.

    python3 tools/atlas_psd.py           writes each atlas, PNG and .json, from its PSD (the build runs it when
                                         a PSD is newer than its PNG)
    python3 tools/atlas_psd.py --init    writes each PSD from its atlas, for a new machine; an existing PSD is
                                         kept, unless --force

In a PSD, each texture is a layer named after its file, in a group per section of the atlas (HUD, Windows…),
then per namespace when the section holds both, then per folder. Section titles, labels and pink frames are on the
"Guides" layer, and "Background" (left out of the atlas) only makes them easier to read. Layers are read as they are:
hiding one, its opacity or its blending mode change nothing.

A texture keeps its frame: only its layer's pixels inside it are read. To add one, add a layer in the group of
its folder, named after its file (without .png), or after its whole path under assets/ (cassonade/textures/gui/
sprites/hud/compass) where no group fits yet. Its frame is its pixels' box, and its file the size of that box.
A layer removed from the PSD removes its texture.

The scripts drawing textures (tools/*_texture*.py) write their PNGs under src/main/resources/assets: open them in
the PSD as layers. Once a texture is in the PSD, the next run deletes its PNG there, so the build does not get it
twice.
"""

import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from psd_tools import PSDImage
from psd_tools.api.layers import Group, PixelLayer

import gui_atlas
import world_atlas

ROOT = Path(__file__).resolve().parent.parent
BACKGROUND = (0xE8, 0xE8, 0xE8, 255)
GUIDES, FILL = "Guides", "Background"
# The group of each section of the atlases, named in plain English
GROUP_NAMES = {
	"HUD": "HUD", "FENETRES": "Windows", "INVENTAIRE ET CREATIF": "Inventory and creative",
	"WIDGETS ET INFOBULLES": "Widgets and tooltips", "LIVRE DE RECETTES": "Recipe book", "CASSONADE": "Cassonade",
	"BLOCS": "Blocks", "OBJETS": "Items", "ENTITES": "Entities", "ICONE": "Icon", "LUNETTE": "Spyglass",
	"VILLAGEOIS": "Villagers", "CIEL": "Sky",
}


class Atlas:
	def __init__(self, png, layout, sections, label, kept_out=()):
		self.png, self.layout, self.sections, self.label = png, layout, sections, label
		self.psd = png.with_suffix(".psd")
		self.kept_out = set(kept_out)

	def section(self, path):
		try:
			return gui_atlas.section(path, self.sections)
		except ValueError:
			return None

	def key(self, path):
		"""Where the texture's layer sits in the PSD: section, then groups, then the layer's name."""
		title, prefixes = self.sections[self.section(path)]
		parts = self.label(path).split("/")
		prefix = next(p for p in prefixes if path.startswith(p))
		groups = [path.split("/", 1)[0]] if len({p.split("/", 1)[0] for p in prefixes}) > 1 else []
		# A section holding a single folder, such as the HUD's sprites/hud/, does not repeat it as a group
		depth = path[len(prefix):].removeprefix("sprites/").count("/")
		return (GROUP_NAMES[title], *groups, *parts[len(parts) - 1 - depth:-1], parts[-1])


ATLASES = [
	Atlas(gui_atlas.ATLAS, gui_atlas.LAYOUT, gui_atlas.SECTIONS, gui_atlas.label),
	Atlas(world_atlas.ATLAS, world_atlas.LAYOUT, world_atlas.SECTIONS, world_atlas.label, world_atlas.KEPT_OUT),
]


def rgba(layer):
	"""The layer's own pixels, as uint8 RGBA, whatever its opacity, visibility or blending."""
	image = layer.topil()
	return None if image is None else np.array(image.convert("RGBA"))


def init(atlas, force):
	if atlas.psd.exists() and not force:
		print(f"{atlas.psd.name} kept: it exists (--force writes it again)")
		return
	pixels = np.array(Image.open(atlas.png).convert("RGBA"))
	layout = json.loads(atlas.layout.read_text())
	height, width = pixels.shape[:2]

	# Tree of groups down to the textures' layers; what is left of the image once they are cut out is the guides
	tree = {}
	guides = pixels.copy()
	for path, texture in layout.items():
		ax, ay, w, h = texture["atlas"]
		*groups, name = atlas.key(path)
		node = tree
		for group in groups:
			node = node.setdefault(group + "/", {})
		node[name] = (ax, ay, pixels[ay:ay + h, ax:ax + w].copy())
		guides[ay:ay + h, ax:ax + w] = 0

	psd = PSDImage.new("RGBA", (width, height))

	# Layers are added from the bottom up: reversed, so the panel lists them in the atlas's order
	def fill(parent, node, top_level=False):
		items = list(node.items()) if top_level else sorted(node.items(), key=lambda i: (not i[0].endswith("/"), i[0]))
		for name, value in reversed(items):
			if isinstance(value, dict):
				fill(Group.new(parent, name.rstrip("/"), open_folder=False), value)
			else:
				left, top, texture = value
				PixelLayer.frompil(Image.fromarray(texture, "RGBA"), parent, name, top, left)

	PixelLayer.frompil(Image.new("RGBA", (width, height), BACKGROUND), psd, FILL, 0, 0)
	fill(psd, tree, top_level=True)
	guides = Image.fromarray(guides, "RGBA")
	box = guides.getbbox()
	PixelLayer.frompil(guides.crop(box), psd, GUIDES, box[1], box[0])
	psd.save(atlas.psd)

	if export(atlas, write=False):
		raise SystemExit(f"{atlas.psd.name}: its layers do not give back {atlas.png.name}")
	print(f"{atlas.psd.name}: {len(layout)} textures, {width}x{height}")


def export(atlas, write=True):
	"""Writes the atlas from its PSD. Returns whether the PNG or the .json would change."""
	psd = PSDImage.open(atlas.psd)
	old_layout = json.loads(atlas.layout.read_text()) if atlas.layout.exists() else {}
	by_key = {atlas.key(path): path for path in old_layout}

	# The textures' layers, and the guides, at their place on the canvas: top layers over bottom ones
	canvas = np.zeros((psd.height, psd.width, 4), np.uint8)
	layers, errors = {}, []

	def paste(layer, pixels):
		top, left = max(layer.top, 0), max(layer.left, 0)
		pixels = pixels[top - layer.top:, left - layer.left:][:psd.height - top, :psd.width - left]
		region = canvas[top:top + pixels.shape[0], left:left + pixels.shape[1]]
		opaque = pixels[..., 3] > 0
		region[opaque] = pixels[opaque]

	def walk(group, names):
		siblings = [layer for layer in group if not layer.is_group()]
		known = {by_key.get((*names, layer.name)) for layer in siblings} - {None}
		for layer in group:
			if layer.is_group():
				walk(layer, (*names, layer.name))
				continue
			if not names and layer.name in (FILL, GUIDES):
				if layer.name == GUIDES and (pixels := rgba(layer)) is not None:
					paste(layer, pixels)
				continue
			path = by_key.get((*names, layer.name))
			if path is None:
				path = new_path(layer.name, known)
			if path is None:
				errors.append(f"layer {'/'.join((*names, layer.name))}: no texture of this name, and no folder to put"
					" a new one in (name it after its path under assets/)")
				continue
			if path in layers:
				errors.append(f"layer {'/'.join((*names, layer.name))}: {path} has two layers")
				continue
			pixels = rgba(layer)
			if pixels is not None:
				paste(layer, pixels)
			layers[path] = layer

	walk(psd, ())
	if errors:
		raise SystemExit(f"{atlas.psd.name}:\n  " + "\n  ".join(errors))

	layout, added = {}, []
	for path, layer in layers.items():
		if path in old_layout:
			texture = old_layout[path]
			ax, ay, w, h = texture["atlas"]
			left, top, right, bottom = layer.bbox
			if left < ax or top < ay or right > ax + w or bottom > ay + h:
				print(f"{atlas.psd.name}: {path} paints outside its frame, those pixels are left out")
			layout[path] = texture
		else:
			left, top, right, bottom = layer.bbox
			if right <= left:
				print(f"{atlas.psd.name}: {path} is empty, left out")
				continue
			layout[path] = {"atlas": [left, top, right - left, bottom - top], "offset": [0, 0],
				"size": [right - left, bottom - top]}
			added.append(path)
	removed = sorted(set(old_layout) - set(layout))
	layout = dict(sorted(layout.items(), key=lambda item: (atlas.section(item[0]), item[0])))

	# A new texture gets the frame and the label the others have, where they find room
	for path in added:
		ax, ay, w, h = layout[path]["atlas"]
		label = atlas.label(path)
		label_top = ay - 1 - gui_atlas.LABEL_GAP - gui_atlas.LABEL_HEIGHT
		label_width = gui_atlas.text_width(label)
		ring = np.zeros(canvas.shape[:2], bool)
		ring[max(ay - 1, 0):ay + h + 1, max(ax - 1, 0):ax + w + 1] = True
		ring[ay:ay + h, ax:ax + w] = False
		if ax >= 1 and ay >= 1 and ay + h < canvas.shape[0] and ax + w < canvas.shape[1] and not canvas[ring, 3].any():
			canvas[ring] = gui_atlas.FRAME_COLOR
		else:
			print(f"{atlas.psd.name}: no room for {path}'s frame")
		if label_top >= 0 and ax - 1 + label_width <= canvas.shape[1] \
				and not canvas[label_top:label_top + gui_atlas.LABEL_HEIGHT, ax - 1:ax - 1 + label_width, 3].any():
			gui_atlas.draw_text(canvas, label, ax - 1, label_top, 1, gui_atlas.LABEL_COLOR)
		else:
			print(f"{atlas.psd.name}: no room for {path}'s label")

	old_pixels = np.array(Image.open(atlas.png).convert("RGBA")) if atlas.png.exists() else None
	if old_pixels is not None:
		# The color of a transparent pixel is not part of the texture
		old_pixels[old_pixels[..., 3] == 0] = 0
	text = json.dumps(layout, indent=1) + "\n"
	changed = old_pixels is None or old_pixels.shape != canvas.shape or not np.array_equal(old_pixels, canvas) \
		or not atlas.layout.exists() or atlas.layout.read_text() != text
	if not write:
		return changed

	if changed:
		Image.fromarray(canvas, "RGBA").save(atlas.png, optimize=True)
		atlas.layout.write_text(text)
		print(f"{atlas.png.name} written from {atlas.psd.name}: {len(layout)} textures")
		for path in added:
			print(f"  + {path}")
		for path in removed:
			print(f"  - {path}")
	else:
		# Newer than the PSD, so the build does not run this again
		atlas.png.touch()
		print(f"{atlas.png.name}: unchanged")

	# The PNGs a script drew, now in the PSD, would reach the build twice
	for path in layout:
		file = gui_atlas.ASSETS / path
		if file.exists():
			file.unlink()
			print(f"  {file.relative_to(ROOT)}: in the PSD, deleted")
	return changed


def new_path(name, known):
	"""The file of a new layer: its whole path under assets/, or its name in the folder of the group's textures."""
	if "/" in name:
		return name.removesuffix(".png") + ".png"
	folders = {path.rsplit("/", 1)[0] for path in known}
	return f"{folders.pop()}/{name.removesuffix('.png')}.png" if len(folders) == 1 else None


def strays():
	"""PNGs drawn by a script under assets, not in the PSDs yet."""
	files = []
	for file in sorted(gui_atlas.ASSETS.glob("**/*.png")):
		path = file.relative_to(gui_atlas.ASSETS).as_posix()
		if any(atlas.section(path) is not None and path not in atlas.kept_out for atlas in ATLASES):
			files.append(path)
	return files


def main(args):
	if "--init" in args:
		for atlas in ATLASES:
			init(atlas, "--force" in args)
		return
	for atlas in ATLASES:
		if atlas.psd.exists():
			export(atlas)
		else:
			print(f"{atlas.psd.name} not found: python3 tools/atlas_psd.py --init writes it from {atlas.png.name}")
	for path in strays():
		print(f"src/main/resources/assets/{path}: not in a PSD yet, open it there as a layer")


if __name__ == "__main__":
	main(sys.argv[1:])
