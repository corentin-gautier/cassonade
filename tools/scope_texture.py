"""Draws the view through a spyglass (textures/misc/spyglass_scope.png, in place of the game's): a round lens
at the end of a dark tube, its ring in the game's scope browns, lit on the bottom right, darker as it sinks into
the black around it; inside, the blue edge of the game's glass and the faint squares of its tint.

The ring is drawn on a 64 pixel grid, each pixel four of the texture's, as coarse as the game's frame; the
glass's blue edge follows it one texture pixel per shade, as finely as the game's. The game's own colors
throughout. The game stretches the texture over the screen's height and fills the rest in black.

Usage:
    python3 tools/scope_texture.py
"""

import math
import pathlib

from pngio import write_png

ROOT = pathlib.Path(__file__).resolve().parent.parent
TARGET = ROOT / "src/main/resources/assets/minecraft/textures/misc/spyglass_scope.png"
GRID = 64
SCALE = 4
CENTER = GRID / 2
# The lens, then the ring around it: its lit edge, its body, its dark band, then the tube's inside
OPENING = 27
LIT_EDGE = 28
BODY = 28.5
DARK_BAND = 29.5
TUBE = 31

# The game's frame, its edge where the light falls, and the tube's inside, darker still, before the black
FRAME_DARK = (49, 30, 10, 255)
FRAME = (64, 41, 16, 255)
FRAME_LIT = (125, 80, 20, 255)
TUBE_INSIDE = (24, 15, 5, 255)
BLACK = (0, 0, 0, 255)
# The glass's edge, from the frame inwards, as the game fades it
GLASS_EDGE = [(30, 63, 88, 238), (45, 78, 121, 204), (69, 98, 147, 120), (131, 186, 223, 39)]
# The glass's tint, in large squares, and its glint
GLASS_TINTS = [(100, 130, 163, 13), (206, 234, 255, 13), (130, 163, 199, 13), (255, 255, 255, 13)]
TINT_SQUARE = 8
GLINT = (249, 252, 255, 81)
GLINT_AT = [(17, 15), (18, 15), (16, 16), (17, 16), (16, 17)]


def cell(x, y):
	"""What a pixel of the grid is: part of the ring or the black around (its color), or None in the lens."""
	px, py = x + 0.5, y + 0.5
	distance = math.hypot(px - CENTER, py - CENTER)
	if distance <= OPENING:
		return None
	if distance > TUBE:
		return BLACK
	if distance > DARK_BAND:
		return TUBE_INSIDE
	# The ring's inner edge catches the light on the bottom right, as the game's frame does
	if distance <= LIT_EDGE and (px - CENTER) + (py - CENTER) > 0:
		return FRAME_LIT
	return FRAME_DARK if distance > BODY else FRAME


def glass(x, y):
	"""The opening's texture pixel: tinted by large squares, with the glint."""
	gx, gy = x // SCALE, y // SCALE
	if (gx, gy) in GLINT_AT:
		return GLINT
	return GLASS_TINTS[(gx // TINT_SQUARE + 2 * (gy // TINT_SQUARE)) % len(GLASS_TINTS)]


def draw():
	grid = [[cell(x, y) for x in range(GRID)] for y in range(GRID)]
	size = GRID * SCALE
	image = [[grid[y // SCALE][x // SCALE] for x in range(size)] for y in range(size)]
	# The glass's edge, ring after ring inwards from the frame, one texture pixel each
	frame = {(x, y) for y in range(size) for x in range(size) if image[y][x] is not None}
	reached = set(frame)
	ring = frame
	for shade in GLASS_EDGE:
		ring = {(x + dx, y + dy) for x, y in ring for dx in (-1, 0, 1) for dy in (-1, 0, 1)
				if 0 <= x + dx < size and 0 <= y + dy < size} - reached
		for x, y in ring:
			image[y][x] = shade
		reached |= ring
	return [[glass(x, y) if pixel is None else pixel for x, pixel in enumerate(row)] for y, row in enumerate(image)]


if __name__ == "__main__":
	TARGET.parent.mkdir(parents=True, exist_ok=True)
	write_png(TARGET, draw())
	print("wrote", TARGET.relative_to(ROOT))
