"""Draws the plants of mushroom fields (textures/block/spore_sprouts, glowcap): plants that grow nowhere else and
look like nothing on the rest of the map, the sprouts in the mycelium's pale lilac with cyan tips, the glowcaps
red and spotted like the fields' own mushrooms. Each is a 16 by 16 cross texture, drawn here pixel by pixel;
then open them in src/textures/world.psd (tools/atlas_psd.py).

Usage:
    python3 tools/mycelium_plant_textures.py
"""

import pathlib

from pngio import write_png

ROOT = pathlib.Path(__file__).resolve().parent.parent
TARGET = ROOT / "src/main/resources/assets/cassonade/textures/block"

PALETTE = {
	".": None,
	# Pale lilac stems, from light to dark, the mycelium's own hues
	"s": (0xD3, 0xC2, 0xE0),
	"m": (0x9E, 0x86, 0xB4),
	"d": (0x68, 0x53, 0x7E),
	# Cyan glows: the sprouts' tips
	"a": (0x8F, 0xF2, 0xE4),
	"A": (0x3F, 0xB8, 0xAE),
	# The glowcaps' caps, the red of the fields' mushrooms, lit from within, with their pale spots
	"c": (0xF4, 0x5C, 0x4C),
	"C": (0xD8, 0x2A, 0x24),
	"K": (0x8C, 0x16, 0x18),
	"x": (0xF6, 0xE6, 0xDC),
	# The glowcaps' thin stems
	"w": (0xE6, 0xDF, 0xEC),
	"v": (0xAF, 0xA2, 0xBE),
}

SPORE_SPROUTS = [
	"................",
	"................",
	"................",
	"....a...........",
	"...aA......a....",
	"....s.....aA....",
	"....m......s..a.",
	"...m..a....m.aA.",
	"...m.aA...m...s.",
	"...d..s...m..m..",
	"..a.d.m..d...m..",
	".aA.d.m..d..d...",
	"..s..dm.d...d...",
	"..m..d.dd..d....",
	"...d.d.d..dd....",
	"...ddd.dd.d.....",
]

GLOWCAP = [
	"................",
	"................",
	"................",
	"......cc........",
	"....cxcCxc......",
	"....KCCCCK......",
	"......wv....cc..",
	"..cc..wv..cxCCc.",
	".cCxc.wv..KCCCK.",
	".KCCK.wv....wv..",
	"...w..wv....wv..",
	"...wv.wv...wv...",
	"...wv.wv...wv...",
	"....v.wv..wv....",
	"....wvwv.wv.....",
	".....wwvwwv.....",
]


def paint(rows):
	assert len(rows) == 16 and all(len(row) == 16 for row in rows), rows
	return [[(0, 0, 0, 0) if PALETTE[c] is None else PALETTE[c] + (255,) for c in row] for row in rows]


def main():
	TARGET.mkdir(parents=True, exist_ok=True)
	for name, rows in [("spore_sprouts", SPORE_SPROUTS), ("glowcap", GLOWCAP)]:
		write_png(TARGET / f"{name}.png", paint(rows))


if __name__ == "__main__":
	main()
