"""The atlas of every texture of the mod outside the interface, src/textures/world.png: blocks, items, the view
through a spyglass, the tavern keeper's clothes, the rain and the mod's icon. It works like src/textures/gui.png
(see tools/gui_atlas.py), written from src/textures/world.psd by tools/atlas_psd.py.
"""

from pathlib import Path

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
# Kept as files: the sea's surface (tools/water_textures.py) is 8192 pixels tall, too tall for the atlas
KEPT_OUT = {"cassonade/textures/block/water_still.png"}


def label(path):
	"""The texture's name in the image: its path under textures, without the extension."""
	return path.split("/", 1)[1].removeprefix("textures/").removesuffix(".png")

