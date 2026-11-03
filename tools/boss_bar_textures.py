#!/usr/bin/env python3
"""Generates the boss bars' sprites (BossBars):

- The frame: an outline, a rim of dark iron lit from the top, and the dark inside the health fills.
- The health, one per vanilla bar color, and the pale trail of the health just lost. Keep their size in
  sync with BossBars.INSIDE_WIDTH and INSIDE_HEIGHT, and the frame's with FRAME_WIDTH and FRAME_HEIGHT.
- The skull held at each end of the bar, its sockets glowing red.

    python3 tools/boss_bar_textures.py
"""

from pathlib import Path

import gui_theme as theme
from gui_theme import rgb
from pngio import write_png

BOSS_BAR = Path(__file__).resolve().parent.parent / "src/main/resources/assets/cassonade/textures/gui/sprites/hud/boss_bar"

INSIDE_WIDTH = 238
INSIDE_HEIGHT = 7
BORDER = 3
OUTLINE = rgb(theme.OUTLINE)
# Bone and its shade, the sockets, their glow
SKULL_COLORS = {"B": rgb(theme.PAPER), "b": rgb(theme.PAPER_SHADOW), "D": rgb(theme.OUTLINE), "r": rgb(theme.BOSS_EYE)}
SKULL = ["...BBBBB...", ".BBBBBBBBb.", "BBBBBBBBBBb", "BBBBBBBBBBb", "BDDDBBBDDDb", "BDrDBBBDrDb", "BBBBBDBBBbb",
	".bBBDDDBBb.", "..BBBBBBb..", "..BDBDBDb..", "..bbbbbbb.."]


def blank(width, height):
	return [[(0, 0, 0, 0)] * width for _ in range(height)]


def frame():
	width, height = INSIDE_WIDTH + 2 * BORDER, INSIDE_HEIGHT + 2 * BORDER
	light, base, dark = rgb(theme.BOSS_FRAME_LIGHT), rgb(theme.BOSS_FRAME), rgb(theme.BOSS_FRAME_DARK)
	pixels = blank(width, height)
	for y in range(height):
		for x in range(width):
			ring = min(x, y, width - 1 - x, height - 1 - y)
			if ring == 1:
				color = light if y == 1 else dark if y == height - 2 else base
			elif ring == BORDER and y == BORDER + INSIDE_HEIGHT - 1:
				color = rgb(theme.RIM_DARK)
			else:
				color = OUTLINE if ring in (0, 2) else rgb(theme.FILL)
			pixels[y][x] = color + (255,)
	return pixels


def health(light, base, dark):
	"""Lit on its top row, shaded on the two bottom ones."""
	pixels = blank(INSIDE_WIDTH, INSIDE_HEIGHT)
	for y in range(INSIDE_HEIGHT):
		color = light if y == 0 else dark if y >= INSIDE_HEIGHT - 2 else base
		pixels[y] = [color + (255,)] * INSIDE_WIDTH
	return pixels


def skull():
	width, height = len(SKULL[0]) + 2, len(SKULL) + 2
	pixels = blank(width, height)
	for y, row in enumerate(SKULL):
		for x, letter in enumerate(row):
			if letter != ".":
				pixels[y + 1][x + 1] = SKULL_COLORS[letter] + (255,)
	art = [[p[3] for p in row] for row in pixels]
	for y in range(height):
		for x in range(width):
			if not art[y][x] and any(0 <= y + dy < height and 0 <= x + dx < width and art[y + dy][x + dx]
					for dy in (-1, 0, 1) for dx in (-1, 0, 1)):
				pixels[y][x] = OUTLINE + (255,)
	return pixels


def main():
	BOSS_BAR.mkdir(parents=True, exist_ok=True)
	write_png(BOSS_BAR / "frame.png", frame())
	for name, (light, base, dark) in theme.BOSS_BAR_COLORS.items():
		write_png(BOSS_BAR / f"{name}.png", health(rgb(light), rgb(base), rgb(dark)))
	trail = rgb(theme.MOB_HEALTH_TRAIL)
	write_png(BOSS_BAR / "trail.png", health(trail, trail, trail))
	write_png(BOSS_BAR / "skull.png", skull())


if __name__ == "__main__":
	main()
