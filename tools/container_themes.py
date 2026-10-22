#!/usr/bin/env python3
"""Gives each vanilla container window a discreet tint and texture after the block it belongs to.

The vanilla backgrounds are drawn with six greys: panel, highlight, shadow, outline, and the inset
fill and shadow of slots and wells. Each block has its own tint, applied to all six so the window
keeps the vanilla contrast, and the panel gets a faint texture of the block's material (wood grain,
stone, brushed metal, paper, cloth). Everything else, arrows, icons and drawings, is kept.

Windows sharing a vanilla texture (chest, barrel and ender chest; dispenser and dropper) get their
own file under the mod's namespace, picked by ContainerThemes.java from the window's title.

    python3 tools/container_themes.py ~/Library/Application\\ Support/minecraft/versions/26.3/26.3.jar
"""

import sys
import zipfile
from pathlib import Path

from pngio import read_png, write_png

ROOT = Path(__file__).resolve().parent.parent
VANILLA_OUTPUT = ROOT / "src/main/resources/assets/minecraft/textures/gui/container"
MOD_OUTPUT = ROOT / "src/main/resources/assets/vanillasugar/textures/gui/container"
SPRITES = ROOT / "src/main/resources/assets/vanillasugar/textures/gui/sprites/container"
VANILLA = "assets/minecraft/textures/gui/container/"

GREYS = PANEL, HIGHLIGHT, SHADOW, OUTLINE, INSET, INSET_SHADOW = (0xC6, 0xFF, 0x55, 0x00, 0x8B, 0x37)


def rgb(value):
	return ((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF)


def clamp(color):
	return tuple(max(0, min(255, round(c))) for c in color)


def hash01(*values):
	"""Deterministic noise in [0, 1) for integer coordinates."""
	h = 2166136261
	for v in values:
		h = ((h ^ (v & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
	h ^= h >> 13
	h = (h * 1274126177) & 0xFFFFFFFF
	return (h ^ (h >> 16)) / 2 ** 32


def smooth_noise(x, y, cell_x, cell_y, seed):
	"""Value noise in [-0.5, 0.5), interpolated between cells of the given size."""
	cx, cy = x // cell_x, y // cell_y
	fx, fy = (x % cell_x) / cell_x, (y % cell_y) / cell_y
	fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
	a, b = hash01(cx, cy, seed), hash01(cx + 1, cy, seed)
	c, d = hash01(cx, cy + 1, seed), hash01(cx + 1, cy + 1, seed)
	return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy - 0.5


def wood(x, y, seed):
	"""Long grain running along x, with no boards or joints."""
	return smooth_noise(x, y, 24, 2, seed) * 7 + smooth_noise(x, y, 6, 1, seed + 1) * 2


def stone(x, y, seed):
	"""Soft mottling."""
	return smooth_noise(x, y, 5, 5, seed) * 6 + (hash01(x, y, seed + 1) - 0.5) * 2


def metal(x, y, seed):
	"""Fine streaks along x."""
	return smooth_noise(x, y, 32, 1, seed) * 4 + (hash01(x // 6, y, seed + 1) - 0.5) * 2


def paper(x, y, seed):
	"""Faint fibres."""
	return smooth_noise(x, y, 10, 10, seed) * 4 - (2 if hash01(x // 3, y, seed + 1) > 0.97 else 0)


def cloth(x, y, seed):
	"""A weave barely there."""
	return (1 if (x + y) % 2 else -1) + smooth_noise(x, y, 8, 8, seed) * 3


def flat(x, y, seed):
	return 0


class Theme:
	"""A tint, the color the vanilla panel grey turns into, and the texture drawn on the panel."""

	def __init__(self, panel, texture=flat, seed=0):
		r, g, b = rgb(panel)
		self.tint = (r / PANEL, g / PANEL, b / PANEL)
		self.texture, self.seed = texture, seed

	def tone(self, grey, amount=0):
		return clamp(grey * t + amount for t in self.tint)

	def recolor(self, color, x, y):
		if color[0] == color[1] == color[2] and color[0] in GREYS:
			return self.tone(color[0], self.texture(x, y, self.seed) if color[0] == PANEL else 0)
		return color


# Output file: (vanilla texture it starts from, theme)
THEMES = {
	"crafting_table": ("crafting_table", Theme(0xC8B597, wood, 1)),
	"generic_54": ("generic_54", Theme(0xCCB28E, wood, 2)),
	"barrel": ("generic_54", Theme(0xBDA88A, wood, 3)),
	"ender_chest": ("generic_54", Theme(0xAFC1BC, stone, 4)),
	"shulker_box": ("shulker_box", Theme(0xC4B4C8, stone, 5)),
	"loom": ("loom", Theme(0xD0C8B0, wood, 6)),
	"cartography_table": ("cartography_table", Theme(0xD8CEB2, paper, 7)),
	"smoker": ("smoker", Theme(0xB9AE9E, wood, 8)),
	"furnace": ("furnace", Theme(0xC0BEBA, stone, 9)),
	"blast_furnace": ("blast_furnace", Theme(0xB8BCC2, metal, 10)),
	"dispenser": ("dispenser", Theme(0xBCC0B8, stone, 11)),
	"dropper": ("dispenser", Theme(0xC2BCB6, stone, 12)),
	"brewing_stand": ("brewing_stand", Theme(0xBCB4BC, stone, 13)),
	"stonecutter": ("stonecutter", Theme(0xC4C4C0, stone, 14)),
	"grindstone": ("grindstone", Theme(0xC6BEB2, stone, 15)),
	"crafter": ("crafter", Theme(0xC2B6B2, stone, 16)),
	"anvil": ("anvil", Theme(0xB4B8BE, metal, 17)),
	"smithing": ("smithing", Theme(0xBAB2AA, metal, 18)),
	"hopper": ("hopper", Theme(0xBEC2C6, metal, 19)),
	"enchanting_table": ("enchanting_table", Theme(0xBEB4CA, stone, 20)),
	"inventory": ("inventory", Theme(0xCCC4B4, cloth, 21)),
}

# Files that replace a vanilla texture of the same name; the others are the mod's own
MOD_ONLY = {"barrel", "ender_chest", "dropper"}


def gear_slot(theme):
	"""An 18 x 18 slot in the inventory's tint, for the gear slots drawn by the mod."""
	pixels = []
	for y in range(18):
		row = []
		for x in range(18):
			if (x == 0 or y == 0) and x < 17 and y < 17:
				grey = INSET_SHADOW
			elif x == 17 or y == 17:
				grey = HIGHLIGHT if x > 0 and y > 0 else INSET
			else:
				grey = INSET
			row.append(theme.tone(grey) + (255,))
		pixels.append(row)
	return pixels


def main(jar_path):
	VANILLA_OUTPUT.mkdir(parents=True, exist_ok=True)
	MOD_OUTPUT.mkdir(parents=True, exist_ok=True)
	with zipfile.ZipFile(jar_path) as jar:
		for name, (source, theme) in THEMES.items():
			pixels = read_png(jar.read(VANILLA + source + ".png"))
			themed = [[theme.recolor(p[:3], x, y) + (p[3],) if p[3] else p for x, p in enumerate(row)] for y, row in enumerate(pixels)]
			write_png((MOD_OUTPUT if name in MOD_ONLY else VANILLA_OUTPUT) / (name + ".png"), themed)
			print(f"{name}: {theme.texture.__name__}")
	write_png(SPRITES / "gear_slot.png", gear_slot(THEMES["inventory"][1]))


if __name__ == "__main__":
	if len(sys.argv) != 2:
		sys.exit(__doc__)
	main(sys.argv[1])
