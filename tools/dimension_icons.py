"""Draws the waystone list's dimension icons (textures/gui/sprites/waystone), in one color like the interface's text.

Usage:
    python3 tools/dimension_icons.py && python3 tools/gui_atlas.py
"""

import pathlib

from pngio import write_png

TARGET = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/cassonade/textures/gui/sprites/waystone"
CREAM = (0xE8, 0xDA, 0xBC, 255)
CLEAR = (0, 0, 0, 0)
# 10 x 10, small next to a button's text
ICONS = {
	# A fir tree
	"overworld": [
		"....XX....",
		"...XXXX...",
		"....XX....",
		"..XXXXXX..",
		"...XXXX...",
		".XXXXXXXX.",
		"..XXXXXX..",
		"....XX....",
		"....XX....",
		"...XXXX...",
	],
	# A flame
	"the_nether": [
		"....X.....",
		"....XX....",
		"...XXX....",
		"...XXXX.X.",
		"..XXXXXXX.",
		"..XXX.XXX.",
		".XXX...XXX",
		".XXX...XXX",
		"..XXX.XXX.",
		"...XXXXX..",
	],
	# An eye of ender
	"the_end": [
		"..........",
		"...XXXX...",
		".XX....XX.",
		"X...XX...X",
		"X..XXXX..X",
		"X..XXXX..X",
		"X...XX...X",
		".XX....XX.",
		"...XXXX...",
		"..........",
	],
	# Elsewhere: a diamond
	"other": [
		"....XX....",
		"...XXXX...",
		"..XX..XX..",
		".XX....XX.",
		"XX......XX",
		"XX......XX",
		".XX....XX.",
		"..XX..XX..",
		"...XXXX...",
		"....XX....",
	],
}


def main():
	TARGET.mkdir(parents=True, exist_ok=True)
	for name, rows in ICONS.items():
		write_png(TARGET / f"{name}.png", [[CREAM if letter == "X" else CLEAR for letter in row] for row in rows])


if __name__ == "__main__":
	main()
