#!/usr/bin/env python3
"""The two texture atlases, src/textures/gui.png and world.png, are written from Photoshop files kept next to
them, gui.psd and world.psd. The PSDs stay on the author's machine (git ignores them): the PNGs and their .json
are what the repository and the build use.

    python3 tools/atlas_psd.py           writes each atlas, PNG and .json, from its PSD (the build runs it when
                                         a PSD is newer than its PNG)
    python3 tools/atlas_psd.py --add     adds the PNGs a script drew under assets as layers in their PSD, each in
                                         the group of its folder, in a free spot near its folder's textures, then
                                         writes the atlases
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


def add(atlas, paths):
	"""Adds each PNG a script drew as a layer in the group of its folder, in a free spot of the atlas with room for its
	frame and label, as near as can be to the other textures of that folder. Returns the paths added, with where the drawn
	pixels sit in the file and the file's size, which a model's texture keeps whole."""
	psd = PSDImage.open(atlas.psd)
	layout = json.loads(atlas.layout.read_text())
	# What is taken: every layer's pixels, the guides included, but not the plain background
	taken = np.zeros((psd.height, psd.width), bool)
	for layer in psd.descendants():
		if layer.is_group() or layer.name == FILL or (pixels := rgba(layer)) is None:
			continue
		top, left = max(layer.top, 0), max(layer.left, 0)
		pixels = pixels[top - layer.top:, left - layer.left:][:psd.height - top, :psd.width - left]
		taken[top:top + pixels.shape[0], left:left + pixels.shape[1]] |= pixels[..., 3] > 0
	added = {}
	for path in paths:
		if path in layout or atlas.section(path) is None:
			continue
		image = Image.open(gui_atlas.ASSETS / path).convert("RGBA")
		size = list(image.size)
		box = image.getbbox()
		if box is None:
			continue
		image = image.crop(box)
		w, h = image.size
		# The frame a pixel around, the label above it, and a pixel of margin all around
		label_width = gui_atlas.text_width(atlas.label(path))
		above = 1 + gui_atlas.LABEL_GAP + gui_atlas.LABEL_HEIGHT + 1
		width, height = max(w + 2, label_width) + 2, above + h + 1 + 1
		*groups, name = atlas.key(path)
		siblings = [texture["atlas"] for other, texture in layout.items() if other.rsplit("/", 1)[0] == path.rsplit("/", 1)[0]]
		near = (np.mean([s[0] for s in siblings]), np.mean([s[1] for s in siblings])) if siblings else (0, 0)
		# Every free spot at once, from a summed-area table of the taken pixels
		sums = np.pad(taken.astype(np.int64).cumsum(0).cumsum(1), ((1, 0), (1, 0)))
		inside = sums[height:, width:] - sums[:-height, width:] - sums[height:, :-width] + sums[:-height, :-width]
		tops, lefts = np.nonzero(inside == 0)
		if len(tops) == 0:
			print(f"{atlas.psd.name}: no room for {path}")
			continue
		best = np.argmin((lefts - near[0]) ** 2 + (tops - near[1]) ** 2)
		left, top = int(lefts[best]), int(tops[best])
		ax, ay = left + 2, top + above + 1
		taken[top:top + height, left:left + width] = True
		# Into the group of its folder, or at the top with its whole path as its name where there is none
		parent = psd
		for group in groups:
			parent = next((layer for layer in parent if layer.is_group() and layer.name == group), None)
			if parent is None:
				break
		if parent is None:
			parent, name = psd, path.removesuffix(".png")
		PixelLayer.frompil(image, parent, name, ay, ax)
		added[path] = ([box[0], box[1]], size)
		print(f"{atlas.psd.name}: + {path} at {ax}, {ay}")
	if added:
		psd.save(atlas.psd)
	return added


def guides(atlas, paths):
	"""Copies the frames and labels the export drew around these new textures into the Guides layer, so the next export
	from the PSD keeps them."""
	if not paths:
		return
	psd = PSDImage.open(atlas.psd)
	old = next(layer for layer in psd if layer.name == GUIDES)
	pixels = np.zeros((psd.height, psd.width, 4), np.uint8)
	own = rgba(old)
	pixels[old.top:old.top + own.shape[0], old.left:old.left + own.shape[1]] = own
	written = np.array(Image.open(atlas.png).convert("RGBA"))
	layout = json.loads(atlas.layout.read_text())
	for path in paths:
		ax, ay, w, h = layout[path]["atlas"]
		top = max(ay - 1 - gui_atlas.LABEL_GAP - gui_atlas.LABEL_HEIGHT, 0)
		right = min(ax - 1 + max(w + 2, gui_atlas.text_width(atlas.label(path))), psd.width)
		region = (slice(top, ay + h + 1), slice(ax - 1, right))
		# The atlas there, but the texture itself, which is its own layer
		copied = written[region].copy()
		copied[ay - top:ay - top + h, 1:1 + w] = 0
		opaque = copied[..., 3] > 0
		pixels[region][opaque] = copied[opaque]
	image = Image.fromarray(pixels, "RGBA")
	box = image.getbbox()
	layer = PixelLayer.frompil(image.crop(box), psd, GUIDES, box[1], box[0])
	psd.remove(layer)
	psd.insert(psd.index(old), layer)
	psd.remove(old)
	psd.save(atlas.psd)
	if export(atlas, write=False):
		raise SystemExit(f"{atlas.psd.name}: its layers do not give back {atlas.png.name}")
	print(f"{atlas.psd.name}: frames and labels of {len(paths)} new textures kept in {GUIDES}")


def main(args):
	if "--add" in args:
		# The PNGs drawn by a script, as layers in their PSD, then the atlases written again
		paths = strays()
		added = {}
		for atlas in ATLASES:
			if atlas.psd.exists():
				added[atlas] = add(atlas, [path for path in paths if atlas.section(path) is not None and path not in atlas.kept_out])
		for atlas in ATLASES:
			if atlas.psd.exists():
				export(atlas)
				placed = added.get(atlas, {})
				if placed:
					# The export makes a new texture's file the size of its pixels: back to the size it was drawn at
					layout = json.loads(atlas.layout.read_text())
					for path, (offset, size) in placed.items():
						layout[path]["offset"], layout[path]["size"] = offset, size
					atlas.layout.write_text(json.dumps(layout, indent=1) + "\n")
				guides(atlas, list(placed))
		return
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
