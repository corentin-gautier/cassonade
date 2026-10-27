#!/usr/bin/env python3
"""Draws the flame and the arrow of the furnaces for the mod's windows: the vanilla sprites with their light gray
background made transparent, so the window's own unlit flame and empty arrow show around what is lit or filled.
The furnace, the blast furnace and the smoker share them (AbstractFurnaceScreenMixin).

    python3 tools/furnace_progress_textures.py [path/to/minecraft-26.3.jar]

The sprites are written under src/main/resources, to be packed into the atlas with python3 tools/gui_atlas.py.
Without a jar, the client jar Loom downloaded for the build is used (run ./gradlew build once first).
"""

import sys
import zipfile
from pathlib import Path

from build_gui import loom_jar
from pngio import read_png, write_png

SPRITES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/minecraft/textures/gui/sprites/container/furnace"
# Vanilla's light gray, the sprites' background
BACKGROUND = (198, 198, 198)
TRANSPARENT = (0, 0, 0, 0)


def main(jar_path):
	SPRITES.mkdir(parents=True, exist_ok=True)
	with zipfile.ZipFile(jar_path) as jar:
		for name in ("lit_progress", "burn_progress"):
			sprite = read_png(jar.read(f"assets/minecraft/textures/gui/sprites/container/furnace/{name}.png"))
			write_png(SPRITES / f"{name}.png", [[TRANSPARENT if pixel[3] and pixel[:3] == BACKGROUND else pixel for pixel in row]
					for row in sprite])


if __name__ == "__main__":
	main(sys.argv[1] if len(sys.argv) > 1 else loom_jar())
