#!/usr/bin/env python3
"""Generates the sprites of the banner shown at the center of the screen when an advancement is made.

- An ornate nine-slice frame for each kind of advancement: wood for a task, gold for a goal, purple
  for a challenge. Keep its border in sync with AchievementBanner.FRAME_BORDER.
- A round medallion of the same colors, where the advancement's icon sits, over the frame's top edge.
- Rays of light, white so the game tints them, turning behind the medallion of goals and challenges.
- The icons of the buttons in the bottom right corner: the quest journal and the advancements.
- The quest journal's leather cover, a nine-slice frame stitched like the backpack; the pages are
  drawn by QuestJournalScreen. Keep its border in sync with QuestJournalScreen.COVER.

    python3 tools/quest_textures.py
"""

import math
from pathlib import Path

import gui_textures
import gui_theme as theme
from gui_theme import rgb
from pngio import write_png

HUD = Path(__file__).resolve().parent.parent / "src/main/resources/assets/cassonade/textures/gui/sprites/hud"

OUTLINE = rgb(theme.OUTLINE)
FILL = rgb(theme.FILL)
# light, base, dark: the band of the frame, lit at the top left
METALS = {
	"task": (rgb(theme.HOVER), rgb(theme.WOOD_LIGHT), rgb(theme.WOOD_DARK)),
	"goal": (rgb(theme.GOLD_LIGHT), rgb(theme.GOLD), rgb(theme.GOLD_DARK)),
	"challenge": (rgb(theme.AMETHYST_LIGHT), rgb(theme.AMETHYST), rgb(theme.AMETHYST_DARK)),
}
FRAME_SIZE = 32
FRAME_BORDER = 10
MEDALLION_SIZE = 28
RAYS_SIZE = 64
RAYS = 12
COVER_SIZE = 48
COVER_BORDER = 12
# Icons of the buttons, 11 x 11, outlined by icon()
ICON_COLORS = {
	"L": rgb(theme.BOOK_COVER), "H": rgb(theme.LEATHER_HIGHLIGHT), "S": rgb(theme.STITCH), "P": rgb(theme.PAPER),
	"R": rgb(theme.RIBBON), "G": rgb(theme.GOLD), "g": rgb(theme.GOLD_LIGHT), "D": rgb(theme.GOLD_DARK),
}
ICONS = {
	# A leather book, its title stitched, its pages on the right and a ribbon hanging out
	"journal": ["HHHHHHHHH..", "HLLLLLLLLP.", "HLSSSSSLLP.", "HLLLLLLLLP.", "HLLSSSLLLP.", "HLLLLLLLLP.",
		"HLLLLLLLLP.", "HLLLLLLLLP.", "HLLLLLLLLP.", "LLLLLRLLLP.", ".....R....."],
	# A golden cup on its stand, its handles on both sides
	"advancements": [".ggGGGGGGD.", "gGgGGGGGDGD", "g.gGGGGGD.D", "g.gGGGGGD.D", ".ggGGGGGDD.", "...gGGGD...",
		"....gGD....", ".....G.....", "....gGD....", "...gGGGD...", "...DDDDD..."],
}


def rgba(color, alpha=255):
	return tuple(color) + (alpha,)


def shade(color, amount):
	return tuple(max(0, min(255, round(c + amount))) for c in color)


def noise(x, y, seed):
	h = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
	h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
	return ((h ^ (h >> 16)) & 0xFF) / 255 - 0.5


def blank(width, height):
	return [[(0, 0, 0, 0)] * width for _ in range(height)]


def frame(metal):
	"""Outline, a lit bevel, a wide band of the metal with a fine line along it, then the dark inside.
	A jewel sits in each corner, where the band turns."""
	light, base, dark = METALS[metal]
	size = FRAME_SIZE
	pixels = blank(size, size)
	for y in range(size):
		for x in range(size):
			ring = min(x, y, size - 1 - x, size - 1 - y)
			lit = x + y < size - 1
			if ring == 0 or ring == 6:
				color = OUTLINE
			elif ring == 1:
				color = light if lit else dark
			elif ring in (2, 3, 4):
				color = shade(base, noise(x, y, 5) * 10)
				if ring == 3:
					color = shade(color, 18 if lit else -18)
			elif ring == 5:
				color = dark if lit else light
			else:
				color = FILL
			pixels[y][x] = rgba(color)
	for cx in (3, size - 4):
		for cy in (3, size - 4):
			jewel(pixels, cx, cy, light, base, dark)
	return pixels


def jewel(pixels, cx, cy, light, base, dark):
	"""A small diamond, outlined, lit at its top left."""
	for dy in range(-3, 4):
		for dx in range(-3, 4):
			distance = abs(dx) + abs(dy)
			if distance == 3:
				pixels[cy + dy][cx + dx] = rgba(OUTLINE)
			elif distance < 3:
				pixels[cy + dy][cx + dx] = rgba(light if dx + dy < 0 else base if dx + dy == 0 else dark)


def medallion(metal):
	"""A disc framed like the banner, its inside dark for the icon."""
	light, base, dark = METALS[metal]
	size = MEDALLION_SIZE
	center = (size - 1) / 2
	pixels = blank(size, size)
	for y in range(size):
		for x in range(size):
			distance = math.hypot(x - center, y - center)
			lit = (x - center) + (y - center) < 0
			if distance > size / 2:
				continue
			if distance > size / 2 - 1:
				color = OUTLINE
			elif distance > size / 2 - 2:
				color = light if lit else dark
			elif distance > size / 2 - 4:
				color = shade(base, noise(x, y, 9) * 10)
			elif distance > size / 2 - 5:
				color = dark if lit else light
			elif distance > size / 2 - 6:
				color = OUTLINE
			else:
				color = FILL
			pixels[y][x] = rgba(color)
	return pixels


def rays():
	"""White rays fading out from the center, the game tints and turns them."""
	size = RAYS_SIZE
	center = (size - 1) / 2
	pixels = blank(size, size)
	for y in range(size):
		for x in range(size):
			dx, dy = x - center, y - center
			distance = math.hypot(dx, dy) / (size / 2)
			if distance >= 1:
				continue
			angle = math.atan2(dy, dx) * RAYS / (2 * math.pi)
			# 1 at the middle of a ray, 0 halfway between two
			ray = max(0.0, math.cos(angle * 2 * math.pi)) ** 1.5
			alpha = ray * (1 - distance) ** 1.5 + 0.25 * (1 - distance) ** 3
			pixels[y][x] = (255, 255, 255, min(255, round(alpha * 255)))
	return pixels


def icon(art):
	"""The art in a 1 pixel dark outline, so it reads on any background."""
	size = len(art) + 2
	pixels = blank(size, size)
	for y, row in enumerate(art):
		for x, letter in enumerate(row):
			if letter != ".":
				pixels[y + 1][x + 1] = rgba(ICON_COLORS[letter])
	for y in range(size):
		for x in range(size):
			if pixels[y][x][3] == 0 and any(0 <= y + dy < size and 0 <= x + dx < size and pixels[y + dy][x + dx][3] == 255
					and pixels[y + dy][x + dx][:3] != OUTLINE for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1))):
				pixels[y][x] = rgba(OUTLINE, 200)
	return pixels


def cover():
	"""Leather with its bevel and a stitched seam; the middle is hidden under the pages."""
	pixels = gui_textures.leather(COVER_SIZE, COVER_SIZE, rgb(theme.BOOK_COVER), seed=31)
	gui_textures.frame(pixels, rgb(theme.LEATHER_OUTLINE))
	gui_textures.stitching(pixels, inset=4)
	return pixels


def main():
	for metal in METALS:
		write_png(HUD / f"banner_{metal}.png", frame(metal))
		write_png(HUD / f"banner_medallion_{metal}.png", medallion(metal))
	write_png(HUD / "banner_rays.png", rays())
	write_png(HUD.parent / "quest/journal_cover.png", cover())
	for name, art in ICONS.items():
		write_png(HUD / f"button_{name}.png", icon(art))
	print("Quest textures written")


if __name__ == "__main__":
	main()
