#!/usr/bin/env python3
"""Draws the bubbles and the arrow of the brewing stand for the mod's window, in the furnace's flame and arrow
palette (tools/furnace_progress_textures.py is for those): the vanilla sprites with their light gray background made
transparent, so the window's own empty bubbles and arrow show around what is filled.

    python3 tools/brewing_progress_textures.py [path/to/minecraft-26.3.jar]

The sprites are written under src/main/resources, to be packed into the atlas with python3 tools/gui_atlas.py.
Without a jar, the client jar Loom downloaded for the build is used (run ./gradlew build once first).
"""

import sys
import zipfile
from pathlib import Path

from build_gui import loom_jar
from pngio import read_png, write_png

SPRITES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/minecraft/textures/gui/sprites/container/brewing_stand"
BACKGROUND = (198, 198, 198)
VANILLA_SHADE = (139, 139, 139)
TRANSPARENT = (0, 0, 0, 0)
YELLOW = (255, 255, 31, 255)
ORANGE = (255, 182, 0, 255)
WHITE = (255, 255, 255, 255)


def recolor(sprite):
	"""Vanilla's white becomes the furnace's yellow, with its white in the middle; its gray shade, the orange."""
	pixels = []
	for row in sprite:
		out = []
		for x, pixel in enumerate(row):
			if not pixel[3] or pixel[:3] == BACKGROUND:
				out.append(TRANSPARENT)
			elif pixel[:3] == VANILLA_SHADE:
				out.append(ORANGE)
			else:
				out.append(YELLOW)
		pixels.append(out)
	return pixels


def main(jar_path):
	SPRITES.mkdir(parents=True, exist_ok=True)
	with zipfile.ZipFile(jar_path) as jar:
		for name in ("bubbles", "brew_progress"):
			sprite = read_png(jar.read(f"assets/minecraft/textures/gui/sprites/container/brewing_stand/{name}.png"))
			write_png(SPRITES / f"{name}.png", recolor(sprite))


if __name__ == "__main__":
	main(sys.argv[1] if len(sys.argv) > 1 else loom_jar())
