#!/usr/bin/env python3
"""Tells the two parts of the container windows apart: the block's part on top, the player's inventory
below, a little darker, a dark seam between them. The block's part takes a muted tint of what the block
is made of, with a faint natural pattern, abstract, so each window can be told at a glance:
leather for the crafting table, rock for the furnace, bark for the smoker, cloth for the loom...
Chests and barrels keep the plain wood, the one of storage; the ender chest gets its own window, dark.

Room is added on both sides of the seam. Above it, a workstation's content gets as much panel below
it as above it, title included, to look centered in its part; a storage's rows get the margin of the
window's sides. Below it, the inventory's label is as far from the seam as the title from the
window's top edge. The windows grow by as much: ContainerPanels moves the inventory's slots and label
down to match, keep SHIFTS in sync with it.

The script edits the textures of textures/gui.png, hand edits included, and leaves a window already
done alone. Then pack them:

    python3 tools/container_panels.py && python3 tools/gui_atlas.py
"""

import random
from pathlib import Path

import gui_atlas
import gui_theme as theme
from gui_theme import rgb
from pngio import write_png

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets"
WIDTH = 176
# Rows between the seam and the inventory's label, like the title below the window's top edge
ROOM_BELOW = 5
# Below a storage's rows, the margin of the window's sides (the seam is its last row)
STORAGE_ROOM = 6


def noise(seed, cell_width, cell_height):
	"""Smooth value noise between -1 and 1: random values on a grid of cells, blended across each cell."""
	rng = random.Random(seed)
	grid = {}

	def corner(cx, cy):
		if (cx, cy) not in grid:
			grid[cx, cy] = rng.uniform(-1, 1)
		return grid[cx, cy]

	def at(x, y):
		fx, fy = x / cell_width, y / cell_height
		cx, cy = int(fx), int(fy)
		tx, ty = fx - cx, fy - cy
		tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
		top = corner(cx, cy) + (corner(cx + 1, cy) - corner(cx, cy)) * tx
		bottom = corner(cx, cy + 1) + (corner(cx + 1, cy + 1) - corner(cx, cy + 1)) * tx
		return top + (bottom - top) * ty
	return at


BARK_WAVE = noise(31, 10, 30)
BARK_FIBER = noise(32, 2, 8)


def bark(x, y):
	"""Smoked bark: faint fissures running down, wavering, the ridges between them barely rough."""
	line = (x + BARK_WAVE(x, y) * 5) % 8
	fissure = -2 if line < 1.2 else -1 if line < 2 else 0
	return fissure + round(BARK_FIBER(x, y))


LEATHER_PEBBLE = noise(81, 2.2, 2.2)
LEATHER_CREASE = noise(82, 18, 11)


def leather(x, y):
	"""Leather: a fine pebbled grain, and a few soft creases."""
	crease = -3 if abs(LEATHER_CREASE(x, y)) < 0.05 else 0
	return round(LEATHER_PEBBLE(x, y) * 2) + crease


OBSIDIAN = noise(91, 7, 5)
_glints = random.Random(92)
GLINTS = {(_glints.randrange(WIDTH), _glints.randrange(256)) for _ in range(120)}


def obsidian(x, y):
	"""Obsidian: dark glass, barely clouded, a few glints here and there."""
	return round(OBSIDIAN(x, y) * 2) + (3 if (x, y) in GLINTS else 0)


ROCK = noise(21, 9, 7)
ROCK_FINE = noise(22, 3, 3)


def rock(x, y):
	"""Rock: soft lighter and darker patches, a fine grit over them."""
	return round(ROCK(x, y) * 3 + ROCK_FINE(x, y) * 1.5)


METAL = noise(41, 30, 1.5)
METAL_DENT = noise(42, 12, 9)


def metal(x, y):
	"""Worn metal: fine streaks across, brushed, and faint dents."""
	return round(METAL(x, y) * 2 + METAL_DENT(x, y) * 1.5)


CLOTH = noise(51, 14, 10)
# Each thread a little lighter or darker than the next, so the weave never looks stamped
_threads = random.Random(52)
WEFT = [_threads.choice((-1, 0, 0, 1)) for _ in range(256)]
WARP = [_threads.choice((-1, 0, 0, 1)) for _ in range(256)]


def cloth(x, y):
	"""Cloth: a fine weave, each pixel the thread on top there, across or down, over soft folds."""
	weave = WEFT[y] if (x + y) % 2 == 0 else WARP[x]
	return weave + round(CLOTH(x, y) * 2)


PAPER_FIBER = noise(61, 5, 1.5)
PAPER_STAIN = noise(62, 20, 16)


def paper(x, y):
	"""Paper: short fibers and broad, faint stains."""
	return round(PAPER_FIBER(x, y) * 1.5 + PAPER_STAIN(x, y) * 2.5)


SHELL = noise(71, 6, 6)


def shell(x, y):
	"""A shulker's shell: smooth, barely mottled."""
	return round(SHELL(x, y) * 2)


# Each window: its texture's namespace, how its content is laid out (workstation or storage), the
# base color of its block's part (None: the wood's) and the pattern on it
WINDOWS = {
	"crafting_table": ("minecraft", "workstation", theme.PANEL_LEATHER, leather),
	"furnace": ("minecraft", "workstation", theme.PANEL_STONE, rock),
	"blast_furnace": ("minecraft", "workstation", theme.PANEL_DARK_STONE, rock),
	"smoker": ("minecraft", "workstation", theme.PANEL_BARK, bark),
	"crafter": ("minecraft", "workstation", theme.PANEL_SLATE, rock),
	"dispenser": ("minecraft", "workstation", theme.PANEL_COBBLESTONE, rock),
	"anvil": ("minecraft", "workstation", theme.PANEL_IRON, metal),
	"grindstone": ("minecraft", "workstation", theme.PANEL_PALE_STONE, rock),
	"smithing": ("minecraft", "workstation", theme.PANEL_SMITHING, metal),
	"stonecutter": ("minecraft", "workstation", theme.PANEL_STONE, rock),
	"enchanting_table": ("minecraft", "workstation", theme.PANEL_RED_CLOTH, cloth),
	"loom": ("minecraft", "workstation", theme.PANEL_WOOL, cloth),
	"cartography_table": ("minecraft", "workstation", theme.PANEL_PAPER, paper),
	"brewing_stand": ("minecraft", "workstation", theme.PANEL_SLATE, rock),
	"generic_54": ("minecraft", "storage", None, None),
	"shulker_box": ("minecraft", "storage", theme.PANEL_SHULKER, shell),
	"hopper": ("minecraft", "storage", theme.PANEL_IRON, metal),
	"vanillasugar:hopper": ("vanillasugar", "storage", theme.PANEL_IRON, metal),
}
# Where the inventory's label is, when not at 72: imageHeight - 94, as in the game. The chest's window
# draws its inventory part from row 126, right below its last row of slots, its label 3 rows lower.
LABELS = {"generic_54": 129, "shulker_box": 73, "hopper": 39, "vanillasugar:hopper": 39}
# How far the inventory moves down, as in ContainerPanels
SHIFTS = {
	"crafting_table": 20, "furnace": 20, "blast_furnace": 20, "smoker": 20, "crafter": 20, "dispenser": 20,
	"anvil": 5, "grindstone": 16, "smithing": 6, "stonecutter": 18, "enchanting_table": 19, "loom": 20,
	"cartography_table": 17, "brewing_stand": 24, "generic_54": 9, "shulker_box": 10, "hopper": 10,
	"vanillasugar:hopper": 10,
}

WOOD_TONES = (rgb(theme.WOOD), rgb(theme.WOOD_LIGHT), rgb(theme.WOOD_DARK))


def shade(color, amount):
	return tuple(max(0, min(255, c + amount)) for c in color)


def tone(pixel):
	"""Which of the wood's tones a pixel is (the texture strays a little around each), or None."""
	if not pixel[3]:
		return None
	for i, color in enumerate(WOOD_TONES):
		if all(abs(c - t) <= 6 for c, t in zip(pixel[:3], color)):
			return i
	return None


def tones(base):
	"""A base color, and its light and dark as far from it as the wood's from the wood."""
	base = rgb(base)
	return [base] + [tuple(b + t - w for b, t, w in zip(base, other, WOOD_TONES[0])) for other in WOOD_TONES[1:]]


def recolor(pixel, i, colors):
	"""The pixel in a new color, keeping its stray from its tone: the texture's hand-made noise."""
	return shade(colors[i], (sum(pixel[:3]) - sum(WOOD_TONES[i])) // 3)


def block_part(rows, base, pattern):
	colors = tones(base)
	out = []
	for y, row in enumerate(rows):
		tinted = []
		for x, pixel in enumerate(row):
			i = tone(pixel)
			if i is None:
				tinted.append(pixel)
			else:
				# The pattern only on the fill, not on the edges nor the lines between the slots
				tinted.append(shade(recolor(pixel, i, colors), pattern(x, y) if i == 0 else 0) + (255,))
		out.append(tinted)
	return out


def inventory_part(rows):
	"""Only the panel's fill darkens, the edges and the lines between the slots stay; its first row is the seam."""
	panel, seam = tones(theme.INVENTORY_PANEL), tones(theme.INVENTORY_SEAM)
	return [[recolor(p, 0, seam if y == 0 else panel) + (255,) if tone(p) == 0 else p
			for p in row] for y, row in enumerate(rows)]


def fresh_rows(rows, models, count, seed):
	"""Plain rows of panel, its edges on the sides, shaped like the models (the rows between the hotbar
	and the window's bottom edge), each pixel's noise drawn anew from the texture's own: copies of
	the same rows, stacked, would repeat."""
	pools = [[p for row in rows for p in row[3:WIDTH - 3] if tone(p) == i] for i in range(3)]
	pools = [pool or [WOOD_TONES[i] + (255,)] for i, pool in enumerate(pools)]
	rng = random.Random(seed)
	return [[rng.choice(pools[i]) if (i := tone(p)) is not None else p for p in models[y % len(models)]]
			for y in range(count)]


def plain(row):
	return all(tone(p) == 0 for p in row[3:WIDTH - 3])


def layout(name, rows, label):
	"""The window's rows rearranged, the row of its seam, and how far the inventory moved down."""
	_, kind, base, pattern = WINDOWS[name]
	bottom = max(y for y, row in enumerate(rows) if any(p[3] for p in row)) + 1
	# The inventory's first slots, below its label, framed from column 7; the block's content may reach
	# beside the label, further right
	slots = next(y for y in range(label, bottom) if tone(rows[y][7]) != 0)
	if name == "generic_54":
		# Its rows, drawn from the top, stop where its inventory part starts
		content_end = 126
	else:
		content_start = next(y for y in range(3, slots) if not plain(rows[y]))
		content_end = max(y for y in range(3, slots) if not plain(rows[y])) + 1
	filler = fresh_rows(rows, rows[bottom - 7:bottom - 3], 40, name)
	room = STORAGE_ROOM if kind == "storage" else content_start
	block = rows[:content_end] + filler[:room]
	if base is not None:
		block = block_part(block, base, pattern)
	inventory = inventory_part(filler[:1 + ROOM_BELOW + slots - label] + rows[slots:bottom])
	result = block + inventory
	return result, len(block), len(result) - bottom


def main():
	textures = gui_atlas.unpack()
	seam_color = rgb(theme.INVENTORY_SEAM)
	for name, (namespace, *_) in WINDOWS.items():
		path = f"{namespace}/textures/gui/container/{name.split(':')[-1]}.png"
		pixels = [list(row) for row in textures[path]]
		rows = [row[:WIDTH] for row in pixels]
		label = LABELS.get(name, 72)
		# Done, the row above the moved label's room is the seam
		seam = rows[label + SHIFTS[name] - ROOM_BELOW - 1][WIDTH // 2]
		if SHIFTS[name] and all(abs(c - s) <= 8 for c, s in zip(seam[:3], seam_color)):
			print(f"{name}: already done, left alone")
			continue
		result, _, shift = layout(name, rows, label)
		if shift != SHIFTS[name]:
			print(f"{name}: the inventory moves by {shift}, not {SHIFTS[name]}: update SHIFTS and ContainerPanels")
			continue
		for y in range(len(pixels)):
			pixels[y][:WIDTH] = result[y] if y < len(result) else [(0, 0, 0, 0)] * WIDTH
		write(path, pixels)
		textures[path] = pixels
	ender_chest(textures)


def write(path, pixels):
	(ASSETS / path).parent.mkdir(parents=True, exist_ok=True)
	write_png(ASSETS / path, pixels)


def ender_chest(textures):
	"""The ender chest's window, its own texture (ContainerScreenMixin): the chest's, its rows on dark obsidian."""
	path = "vanillasugar/textures/gui/container/ender_chest.png"
	if path in textures:
		print("ender_chest: already done, left alone")
		return
	chest = [list(row) for row in textures["minecraft/textures/gui/container/generic_54.png"]]
	# Above the seam, below the chest's part drawn from row 126
	top = 126 + STORAGE_ROOM
	write(path, block_part(chest[:top], theme.PANEL_ENDER, obsidian) + chest[top:])


if __name__ == "__main__":
	main()
