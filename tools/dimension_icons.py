"""Draws the waystone list's dimension icons (textures/gui/sprites/waystone), in one color like the interface's text,
and the waystone's icon on the world map (textures/gui/map), in the block's colors over a dark line for any terrain.

Usage:
    python3 tools/dimension_icons.py, then open the PNGs in src/textures/gui.psd (tools/atlas_psd.py)
"""

import pathlib

from pngio import write_png

GUI = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/cassonade/textures/gui"
TARGET = GUI / "sprites/waystone"
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


# 8 x 8 like the vanilla map decorations: the pink cube over the stone
MAP_COLORS = {
	".": CLEAR,
	"K": (0x1C, 0x11, 0x09, 255),
	"P": (0xF0, 0x7A, 0xD0, 255),
	"S": (0xA8, 0xA4, 0x9C, 255),
}
MAP_ICON = [
	"...KK...",
	"..KPPK..",
	"..KPPK..",
	"...KK...",
	"..KSSK..",
	"..KSSK..",
	".KSSSSK.",
	".KKKKKK.",
]


def main():
	(GUI / "map").mkdir(parents=True, exist_ok=True)
	write_png(GUI / "map/waystone.png", [[MAP_COLORS[letter] for letter in row] for row in MAP_ICON])
	TARGET.mkdir(parents=True, exist_ok=True)
	for name, rows in ICONS.items():
		write_png(TARGET / f"{name}.png", [[CREAM if letter == "X" else CLEAR for letter in row] for row in rows])


if __name__ == "__main__":
	main()
