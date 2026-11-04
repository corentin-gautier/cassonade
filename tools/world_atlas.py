#!/usr/bin/env python3
"""Packs every texture of the mod outside the interface into one image, src/textures/world.png: blocks, items, the view
through a spyglass, the tavern keeper's clothes, the rain and the mod's icon. src/textures/gui.png holds the interface; this image works the same way, cut back into
the game's files by the build, following src/textures/world.json (see tools/gui_atlas.py).

    python3 tools/world_atlas.py     packs the PNGs found under src/main/resources, then removes them;
                                     with none, lays the image out again

The scripts drawing these textures (tools/*_texture*.py) write PNGs under src/main/resources: run this after them.
"""

from pathlib import Path

from gui_atlas import ASSETS, pack

ROOT = Path(__file__).resolve().parent.parent
ATLAS = ROOT / "src/textures/world.png"
LAYOUT = ROOT / "src/textures/world.json"
SECTIONS = [
	("BLOCS", ["cassonade/textures/block/"]),
	("OBJETS", ["cassonade/textures/item/"]),
	("ENTITES", ["cassonade/textures/entity/"]),
	("ICONE", ["cassonade/icon.png"]),
	("LUNETTE", ["minecraft/textures/misc/"]),
	("VILLAGEOIS", ["minecraft/textures/entity/"]),
	("CIEL", ["minecraft/textures/environment/", "cassonade/textures/particle/"]),
]
WIDTH = 512


def label(path):
	"""The texture's name in the image: its path under textures, without the extension."""
	return path.split("/", 1)[1].removeprefix("textures/").removesuffix(".png")


if __name__ == "__main__":
	files = sorted(file for file in ASSETS.glob("**/*.png") if "/textures/gui/" not in file.as_posix())
	pack(files, ATLAS, LAYOUT, SECTIONS, label, WIDTH)
