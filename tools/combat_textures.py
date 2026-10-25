#!/usr/bin/env python3
"""Generates the sprites drawn above the mobs in a fight (CombatHud):

- The health bar: its dark background, framed like the HUD's bars, the health left in red, and a
  pale trail for the health just lost. Keep its size in sync with CombatHud.BAR_WIDTH and BAR_HEIGHT.
- The "!" shown over a mob that chases the player, gold in a dark outline so it reads on any sky.
- The eye shown next to the hotbar while mobs chase the player, its iris red like their health.

    python3 tools/combat_textures.py
"""

from pathlib import Path

import gui_theme as theme
from gui_theme import rgb
from pngio import write_png

HUD = Path(__file__).resolve().parent.parent / "src/main/resources/assets/cassonade/textures/gui/sprites/hud"

OUTLINE = rgb(theme.OUTLINE)
FILL = rgb(theme.FILL)
BAR_WIDTH = 32
BAR_HEIGHT = 5
SPOTTED_COLORS = {"g": rgb(theme.GOLD_LIGHT), "G": rgb(theme.GOLD), "D": rgb(theme.GOLD_DARK)}
# Lit from the left like the HUD's icons, the stem narrowing down to the dot
SPOTTED = ["gGGD", "gGGD", "gGGD", "gGGD", ".gD.", ".gD.", "....", "gGGD", "gGGD"]
# Lid, white and its shade, iris, its glint, pupil
EYE_COLORS = {
	"L": rgb(theme.OUTLINE), "W": rgb(theme.PAPER), "w": rgb(theme.PAPER_SHADOW),
	"I": rgb(theme.MOB_HEALTH), "h": rgb(theme.MOB_HEALTH_LIGHT), "P": rgb(theme.OUTLINE),
}
EYE = ["....LLLLLLL....", "..LLWWIIIWWLL..", ".LWWWIhIIIWWWL.", "LWWWWIPPPIWWWWL", "LwWWWIPPPIWWWwL",
	".LwWWIIIIIWWwL.", "..LLwwIIIwwLL..", "....LLLLLLL...."]


def blank(width, height):
	return [[(0, 0, 0, 0)] * width for _ in range(height)]


def bar(light, base, dark, background):
	"""The bar's inside, three rows lit at the top; the background also draws the outline around it."""
	pixels = blank(BAR_WIDTH, BAR_HEIGHT)
	for y in range(BAR_HEIGHT):
		for x in range(BAR_WIDTH):
			if x in (0, BAR_WIDTH - 1) or y in (0, BAR_HEIGHT - 1):
				if background:
					pixels[y][x] = OUTLINE + (255,)
			else:
				pixels[y][x] = (light if y == 1 else dark if y == BAR_HEIGHT - 2 else base) + (255,)
	return pixels


def spotted():
	width, height = len(SPOTTED[0]) + 2, len(SPOTTED) + 2
	pixels = blank(width, height)
	for y, row in enumerate(SPOTTED):
		for x, letter in enumerate(row):
			if letter != ".":
				pixels[y + 1][x + 1] = SPOTTED_COLORS[letter] + (255,)
	art = [[p[3] for p in row] for row in pixels]
	for y in range(height):
		for x in range(width):
			if not art[y][x] and any(0 <= y + dy < height and 0 <= x + dx < width and art[y + dy][x + dx]
					for dy in (-1, 0, 1) for dx in (-1, 0, 1)):
				pixels[y][x] = OUTLINE + (255,)
	return pixels


def eye():
	pixels = blank(len(EYE[0]), len(EYE))
	for y, row in enumerate(EYE):
		for x, letter in enumerate(row):
			if letter != ".":
				pixels[y][x] = EYE_COLORS[letter] + (255,)
	return pixels


def main():
	HUD.mkdir(parents=True, exist_ok=True)
	write_png(HUD / "mob_health_background.png", bar(FILL, FILL, rgb(theme.RIM_DARK), True))
	write_png(HUD / "mob_health_progress.png",
		bar(rgb(theme.MOB_HEALTH_LIGHT), rgb(theme.MOB_HEALTH), rgb(theme.MOB_HEALTH_DARK), False))
	trail = rgb(theme.MOB_HEALTH_TRAIL)
	write_png(HUD / "mob_health_trail.png", bar(trail, trail, trail, False))
	write_png(HUD / "spotted.png", spotted())
	write_png(HUD / "hunted.png", eye())


if __name__ == "__main__":
	main()
