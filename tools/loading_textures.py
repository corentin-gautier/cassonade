#!/usr/bin/env python3
"""Generates the floppy disk shown while a world saves (WorldLoading), a game pixel per pixel, in the
wood and paper of the mod's panels. Keep its size in sync with WorldLoading.SAVE_ICON_SIZE.

    python3 tools/loading_textures.py
"""

from pathlib import Path

import gui_theme as theme
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


def floppy():
	return [[COLORS[c] + (255,) if c in COLORS else (0, 0, 0, 0) for c in row] for row in FLOPPY]


def main():
	LOADING.mkdir(parents=True, exist_ok=True)
	write_png(LOADING / "save.png", floppy())


if __name__ == "__main__":
	main()
