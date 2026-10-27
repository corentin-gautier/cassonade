"""Copies the icons the website's crafting grids show (site/assets/items): the vanilla ingredients from the
game's textures, the crafted items from the mod's own.

The barometer, the sculk compass, the naturalist's spyglass and the sculk torch are drawn by gear_item_textures.py, which writes
their copies itself. The blocks (lightning rod, sculk sensor) have no flat texture: the website's screenshots
test draws them as the game does (SiteScreenshotsClientGameTest).

Usage:
    python3 tools/site_recipe_icons.py ~/Library/Application\\ Support/minecraft/versions/26.3/26.3.jar
"""

import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
TARGET = ROOT / "site/assets/items"
# The icon's name, and the texture the game shows it with when it changes with the time or the place
VANILLA = {"leather": "leather", "string": "string", "bundle": "bundle", "stick": "stick", "map": "map", "book": "book",
	"spyglass": "spyglass", "clock": "clock_00", "compass": "compass_16"}
# Items the game shows with their block's texture
VANILLA_BLOCKS = {"torch": "torch"}
MOD = ["backpack", "world_map"]


def main(jar_path):
	TARGET.mkdir(parents=True, exist_ok=True)
	with zipfile.ZipFile(jar_path) as jar:
		for name, texture in VANILLA.items():
			(TARGET / f"{name}.png").write_bytes(jar.read(f"assets/minecraft/textures/item/{texture}.png"))
		for name, texture in VANILLA_BLOCKS.items():
			(TARGET / f"{name}.png").write_bytes(jar.read(f"assets/minecraft/textures/block/{texture}.png"))
	for name in MOD:
		(TARGET / f"{name}.png").write_bytes((ROOT / f"src/main/resources/assets/cassonade/textures/item/{name}.png").read_bytes())


if __name__ == "__main__":
	if len(sys.argv) != 2:
		sys.exit(__doc__)
	main(sys.argv[1])
