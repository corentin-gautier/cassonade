#!/usr/bin/env python3
"""Redraws every GUI texture from tools/gui_theme.py and packs them into textures/gui.png, and copies
the theme's text colors into the mixins that draw text. Edits made by hand in textures/gui.png are
replaced: to keep them, edit the image only, or commit it first.

    python3 tools/build_gui.py [path/to/minecraft-26.3.jar]

Without a jar, the client jar Loom downloaded for the build is used (run ./gradlew build once first).
"""

import re
import sys
from pathlib import Path

import combat_textures
import enchanting_book_textures
import gui_atlas
import gui_style
import gui_textures
import gui_theme
import panel_textures
import quest_textures
import recipe_book_textures

ROOT = Path(__file__).resolve().parent.parent
MIXINS = ROOT / "src/client/java/com/cassonade/client/mixin"
# Java constant -> the theme color it takes
TEXT_COLORS = [
	(MIXINS / "ContainerLabelsMixin.java", "CREAM", gui_theme.LABEL),
	(MIXINS / "RecipeBookPageMixin.java", "INK", gui_theme.INK),
]


def loom_jar():
	jars = sorted(p for p in (ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft").glob("minecraft-clientOnly-*/*/*.jar")
		if not p.name.endswith("-sources.jar"))
	if not jars:
		sys.exit("No Minecraft jar found: run ./gradlew build once, or pass the jar's path.\n\n" + __doc__)
	return jars[-1]


def sync_text_color(path, constant, color):
	source = path.read_text()
	updated = re.sub(rf"({constant} = 0xFF)[0-9A-Fa-f]{{6}};", rf"\g<1>{color[1:].upper()};", source)
	if updated != source:
		path.write_text(updated)
		print(f"{path.name}: {constant} = {color}")


def main():
	jar = sys.argv[1] if len(sys.argv) > 1 else str(loom_jar())
	gui_style.main(jar)
	gui_textures.main()
	quest_textures.main()
	enchanting_book_textures.main()
	recipe_book_textures.main(jar)
	combat_textures.main()
	gui_atlas.pack()
	# Reads the minimap's frame from the image just packed
	panel_textures.main()
	gui_atlas.pack()
	for path, constant, color in TEXT_COLORS:
		sync_text_color(path, constant, color)


if __name__ == "__main__":
	main()
