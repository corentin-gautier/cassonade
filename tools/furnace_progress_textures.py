#!/usr/bin/env python3
"""Draws the flame and the arrow of the furnace, blast furnace and smoker on the mod's own windows: the vanilla
sprites, their light gray background replaced by the window's pixels under them, so the parts not yet lit or
filled match the window around them.

    python3 tools/furnace_progress_textures.py [path/to/minecraft-26.3.jar]

The windows are read from the atlas as the build unpacks it (run ./gradlew build once first); the sprites
are written under src/main/resources, to be packed into the atlas with python3 tools/gui_atlas.py.
Without a jar, the client jar Loom downloaded for the build is used.
"""

import sys
import zipfile
from pathlib import Path

from build_gui import loom_jar
from pngio import read_png, write_png

ROOT = Path(__file__).resolve().parent.parent
WINDOWS = ROOT / "build/generated/guiTextures/assets/minecraft/textures/gui/container"
SPRITES = ROOT / "src/main/resources/assets/minecraft/textures/gui/sprites/container"
# Vanilla's light gray, the sprites' background
BACKGROUND = (198, 198, 198)
# Each sprite, and where AbstractFurnaceScreen draws it in the window
PLACES = {"lit_progress": (56, 36), "burn_progress": (79, 34)}


def on_window(sprite, window, left, top):
	return [[window[top + y][left + x] if pixel[3] and pixel[:3] == BACKGROUND else pixel
			for x, pixel in enumerate(row)] for y, row in enumerate(sprite)]


def main(jar_path):
	with zipfile.ZipFile(jar_path) as jar:
		for furnace in ("furnace", "blast_furnace", "smoker"):
			window = read_png((WINDOWS / f"{furnace}.png").read_bytes())
			for name, (left, top) in PLACES.items():
				sprite = read_png(jar.read(f"assets/minecraft/textures/gui/sprites/container/{furnace}/{name}.png"))
				target = SPRITES / furnace / f"{name}.png"
				target.parent.mkdir(parents=True, exist_ok=True)
				write_png(target, on_window(sprite, window, left, top))


if __name__ == "__main__":
	main(sys.argv[1] if len(sys.argv) > 1 else loom_jar())
