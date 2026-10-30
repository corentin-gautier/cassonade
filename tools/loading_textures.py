#!/usr/bin/env python3
"""Generates the floppy disk shown while a world saves (WorldLoading), a game pixel per pixel, in the
wood and paper of the mod's panels, and the grained background of the loading screens and the world
selection. Keep the disk's size in sync with WorldLoading.SAVE_ICON_SIZE, the background's with its .mcmeta.

    python3 tools/loading_textures.py
"""

from pathlib import Path

import gui_theme as theme
from gui_style import noise, shade
from gui_theme import rgb
from pngio import write_png

LOADING = Path(__file__).resolve().parent.parent / "src/main/resources/assets/cassonade/textures/gui/sprites/loading"

COLORS = {
	"O": rgb(theme.OUTLINE), "B": rgb(theme.WOOD), "L": rgb(theme.WOOD_LIGHT), "D": rgb(theme.WOOD_DARK),
	"S": rgb(theme.PAPER_SHADOW), "P": rgb(theme.PAPER), "I": rgb(theme.INK),
}
# The metal shutter with its window at the top, the written label below, lit from the left
FLOPPY = [
	"OOOOOOOOOOOOOO..",
	"OBLSSSSSSSSSLBO.",
	"OLBSSSSIISSSBBBO",
	"OLBSSSSIISSSBBBO",
	"OLBSSSSIISSSBBBO",
	"OLBBSSSSSSSSBBBO",
	"OLBBBBBBBBBBBBBO",
	"OLBBBBBBBBBBBBBO",
	"OLBPPPPPPPPPPBBO",
	"OLBPIIIIIIIIPBBO",
	"OLBPPPPPPPPPPBBO",
	"OLBPIIIIIIIIPBBO",
	"OLBPPPPPPPPPPBBO",
	"OLBPIIIIIIPPPBBO",
	"ODDDDDDDDDDDDDDO",
	"OOOOOOOOOOOOOOOO",
]


BACKGROUND = (25, 24, 24)
BACKGROUND_GRAIN = 6
BACKGROUND_SIZE = 64


def floppy():
	return [[COLORS[c] + (255,) if c in COLORS else (0, 0, 0, 0) for c in row] for row in FLOPPY]


def background():
	"""As large as a tile can be without weighing anything, so that its grain never shows a pattern."""
	return [[shade(BACKGROUND, noise(x, y, 26) * BACKGROUND_GRAIN) + (255,) for x in range(BACKGROUND_SIZE)] for y in range(BACKGROUND_SIZE)]


def main():
	LOADING.mkdir(parents=True, exist_ok=True)
	write_png(LOADING / "save.png", floppy())
	write_png(LOADING / "background.png", background())


if __name__ == "__main__":
	main()
