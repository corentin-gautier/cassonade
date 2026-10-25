"""Copies the icons the website's crafting grids show (site/assets/items): the vanilla ingredients from the
game's textures, the crafted items from the mod's own.

The barometer and the sculk compass are drawn by gear_item_textures.py, which writes their copies itself.

Usage:
    python3 tools/site_recipe_icons.py ~/Library/Application\\ Support/minecraft/versions/26.3/26.3.jar
"""

import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
TARGET = ROOT / "site/assets/items"
VANILLA = ["leather", "string", "bundle", "stick", "map"]
MOD = ["backpack", "world_map"]


def main(jar_path):
	TARGET.mkdir(parents=True, exist_ok=True)
	with zipfile.ZipFile(jar_path) as jar:
		for name in VANILLA:
			(TARGET / f"{name}.png").write_bytes(jar.read(f"assets/minecraft/textures/item/{name}.png"))
	for name in MOD:
		(TARGET / f"{name}.png").write_bytes((ROOT / f"src/main/resources/assets/cassonade/textures/item/{name}.png").read_bytes())


if __name__ == "__main__":
	if len(sys.argv) != 2:
		sys.exit(__doc__)
	main(sys.argv[1])
