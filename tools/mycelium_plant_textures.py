"""Draws the plants of mushroom fields (textures/block/spore_sprouts, glowcap, puffstalk_bottom, puffstalk_top):
plants that grow nowhere else and look like nothing on the rest of the map, in the mycelium's pale lilac with
cyan glows. Each is a 16 by 16 cross texture, drawn here pixel by pixel; then run tools/world_atlas.py.

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
	# Cyan glows: the sprouts' tips and the glowcaps' caps
	"a": (0x8F, 0xF2, 0xE4),
	"A": (0x3F, 0xB8, 0xAE),
	"c": (0xA6, 0xFA, 0xEF),
	"C": (0x46, 0xD6, 0xCA),
	"K": (0x1F, 0x86, 0x88),
	# The glowcaps' thin stems
	"w": (0xE6, 0xDF, 0xEC),
	"v": (0xAF, 0xA2, 0xBE),
	# The puffstalk's stalk, cream with violet rings
	"p": (0xDE, 0xD2, 0xC4),
	"q": (0xB6, 0xA4, 0x92),
	"r": (0x86, 0x72, 0x63),
	"b": (0x8A, 0x5A, 0xAE),
	"B": (0x5E, 0x37, 0x80),
	# The puff, a violet ball with pale spots
	"u": (0xC8, 0x9E, 0xE6),
	"U": (0xA2, 0x72, 0xD0),
	"V": (0x70, 0x42, 0x9C),
	"W": (0x4C, 0x28, 0x72),
	"o": (0xEE, 0xE2, 0xF8),
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
	"....cccCCc......",
	"....KCCCCK......",
	"......wv....cc..",
	"..cc..wv..cCCCc.",
	".cCCc.wv..KCCCK.",
	".KCCK.wv....wv..",
	"...w..wv....wv..",
	"...wv.wv...wv...",
	"...wv.wv...wv...",
	"....v.wv..wv....",
	"....wvwv.wv.....",
	".....wwvwwv.....",
]

PUFFSTALK_BOTTOM = [
	"......pqqr......",
	"......pqqr......",
	"......bbbB......",
	"......pqqr......",
	"..s...pqqr......",
	"...m..pqqr......",
	"....m.bbbB......",
	".....mpqqr..s...",
	"......pqqr.m....",
	"......pqqrm.....",
	"......bbbB......",
	"......pqqr......",
	".....ppqqrr.....",
	".....pqqqqr.....",
	"....ppqqqqrr....",
	"...pppqqqqrrr...",
]

PUFFSTALK_TOP = [
	"................",
	".....UUUUU......",
	"...UUuuuuUUV....",
	"..UuuouuuuuUV...",
	"..UuooUuuouUV...",
	".UuuuuuuuuuUUV..",
	".UuuuUuuouuuUV..",
	".VUuuuuuuuuUVV..",
	"..VUUuuuuUUVW...",
	"...VVUUUUVVW....",
	".....WVVWW......",
	"......pqqr......",
	"......pqqr......",
	"......bbbB......",
	"......pqqr......",
	"......pqqr......",
]


def paint(rows):
	assert len(rows) == 16 and all(len(row) == 16 for row in rows), rows
	return [[(0, 0, 0, 0) if PALETTE[c] is None else PALETTE[c] + (255,) for c in row] for row in rows]


def main():
	TARGET.mkdir(parents=True, exist_ok=True)
	for name, rows in [("spore_sprouts", SPORE_SPROUTS), ("glowcap", GLOWCAP), ("puffstalk_bottom", PUFFSTALK_BOTTOM),
			("puffstalk_top", PUFFSTALK_TOP)]:
		write_png(TARGET / f"{name}.png", paint(rows))


if __name__ == "__main__":
	main()
