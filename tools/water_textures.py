"""Draws the still water's surface over several blocks, so the sea doesn't show the same 16 pixels every block:

- textures/block/water_still.png: BLOCKS by BLOCKS blocks of 16 pixels, FRAMES frames looping, in the grey levels and
  alpha of the game's own water_still, which the biome then tints.

The ripples come from the water of Minecraft's early versions, the one the game's texture was drawn from: each pixel
takes the mean of its row neighbours, so ripples stretch along X, plus what drops falling at random give it. Drops
follow a schedule repeating every FRAMES steps; the water forgets its past, so after a few loops it repeats too and
the last loop closes on itself. The grid wraps around, so the texture tiles without a seam.
FluidStateModelSetMixin gives the water this sprite and FluidRendererMixin each block its share of it; the game's
water_still is left to cauldrons and resource packs.

Usage:
    python3 tools/water_textures.py
"""

import json
import pathlib
import random

from pngio import write_png

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/cassonade/textures/block/water_still.png"

BLOCKS = 8
SIZE = 16 * BLOCKS
FRAMES = 64
# One step of the automaton a frame, each shown for FRAMETIME ticks as the game's water does
FRAMETIME = 2
# How far a ripple runs along its row: the early game's 3.3 drew streaks longer than the game's water has now
SPREAD = 3.8
WARMUP = 12

# The game's water: grey levels and how much of the surface each covers, darkest first
LEVELS = [(165, 0.405), (174, 0.38), (194, 0.135), (211, 0.05), (255, 0.03)]
ALPHA = 180


def simulate():
	rng = random.Random(7)
	n = SIZE * SIZE
	drops = [[i for i in range(n) if rng.random() < 0.05] for _ in range(FRAMES)]
	red, blue, heat = [0.0] * n, [0.0] * n, [0.0] * n
	frames = []
	for loop in range(WARMUP):
		for step in range(FRAMES):
			green = [0.0] * n
			for y in range(SIZE):
				row = y * SIZE
				for x in range(SIZE):
					total = red[row + (x - 1) % SIZE] + red[row + x] + red[row + (x + 1) % SIZE]
					green[row + x] = total / SPREAD + blue[row + x] * 0.8
			for i in range(n):
				blue[i] = max(0.0, blue[i] + heat[i] * 0.05)
				heat[i] -= 0.1
			for i in drops[step]:
				heat[i] = 0.5
			red = green
			if loop == WARMUP - 1:
				frames.append([min(1.0, max(0.0, v)) ** 2 for v in red])
	return frames


def quantize(frames):
	values = sorted(v for frame in frames for v in frame)
	bounds, share = [], 0.0
	for _, part in LEVELS[:-1]:
		share += part
		bounds.append(values[int(share * (len(values) - 1))])

	def level(v):
		for bound, (grey, _) in zip(bounds, LEVELS):
			if v <= bound:
				return grey
		return LEVELS[-1][0]

	return [[[(level(v), level(v), level(v), ALPHA) for v in frame[y * SIZE:(y + 1) * SIZE]] for y in range(SIZE)] for frame in frames]


if __name__ == "__main__":
	frames = quantize(simulate())
	OUT.parent.mkdir(parents=True, exist_ok=True)
	write_png(OUT, [row for frame in frames for row in frame])
	OUT.with_name(OUT.name + ".mcmeta").write_text(json.dumps({"animation": {"frametime": FRAMETIME}}, indent="\t") + "\n")
