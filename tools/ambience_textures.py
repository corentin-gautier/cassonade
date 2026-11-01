"""Draws the bird, gull and butterfly (textures/entity), in the box layout of client/ambience/Birds and Butterflies. The
butterfly's wings are light greys, tinted by the game to each one's color.

Usage:
    python3 tools/ambience_textures.py && python3 tools/world_atlas.py
"""

import pathlib
import random

from pngio import write_png

TARGET = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/cassonade/textures/entity"


def box(image, u, v, width, height, depth, top, bottom, sides):
	"""Fills a box's faces where the game maps them: top and bottom in the first row, the sides around below."""
	def fill(x, y, w, h, colors):
		for py in range(y, y + h):
			for px in range(x, x + w):
				image[py][px] = (*RANDOM.choice(colors), 255)
	fill(u + depth, v, width, depth, top)
	fill(u + depth + width, v, width, depth, bottom)
	fill(u, v + depth, 2 * (width + depth), height, sides)


RANDOM = random.Random(7)
DARK = [(46, 42, 44), (54, 49, 50), (39, 36, 38)]
BACK = [(70, 62, 58), (78, 69, 63), (62, 55, 52)]
BELLY = [(172, 164, 150), (186, 178, 163), (158, 150, 138)]
BEAK = [(214, 140, 44), (196, 122, 36)]
HEAD = [(30, 28, 30), (37, 34, 36)]


def bird():
	image = [[(0, 0, 0, 0)] * 64 for _ in range(32)]
	box(image, 0, 0, 4, 3, 8, BACK, BELLY, BACK)
	box(image, 0, 12, 3, 3, 3, HEAD, HEAD, HEAD)
	box(image, 12, 12, 1, 1, 2, BEAK, BEAK, BEAK)
	box(image, 0, 19, 4, 1, 4, DARK, DARK, DARK)
	box(image, 24, 0, 6, 1, 5, BACK, BELLY, DARK)
	box(image, 24, 7, 6, 1, 4, DARK, DARK, DARK)
	return image


WHITE = [(236, 236, 230), (224, 224, 218), (244, 244, 238)]
GREY = [(150, 156, 162), (138, 144, 150), (160, 166, 172)]
TIP = [(40, 40, 44), (52, 52, 56)]
YELLOW = [(226, 190, 60), (210, 172, 48)]


def gull():
	"""The bird's boxes: white with a grey back and black wing tips, as a gull seen from below is mostly white."""
	image = [[(0, 0, 0, 0)] * 64 for _ in range(32)]
	box(image, 0, 0, 4, 3, 8, GREY, WHITE, WHITE)
	box(image, 0, 12, 3, 3, 3, WHITE, WHITE, WHITE)
	box(image, 12, 12, 1, 1, 2, YELLOW, YELLOW, YELLOW)
	box(image, 0, 19, 4, 1, 4, WHITE, WHITE, WHITE)
	box(image, 24, 0, 6, 1, 5, GREY, WHITE, GREY)
	box(image, 24, 7, 6, 1, 4, TIP, TIP, TIP)
	return image


def butterfly():
	image = [[(0, 0, 0, 0)] * 32 for _ in range(16)]
	# Each wing a flat shape, both sides of its box's top and bottom: dark edge, a pale spot
	for u in (0, 6):
		for y, row in enumerate(("#####.", "#ooo##", "#o+oo#", "#ooo#.", "##oo#.", ".###..")):
			for x, pixel in enumerate(row):
				color = {"#": (70, 70, 70), "o": (235, 235, 235), "+": (255, 255, 255)}.get(pixel)
				if color:
					image[y][u + x] = (*color, 255)
	box(image, 0, 8, 1, 1, 4, [(50, 45, 45)], [(50, 45, 45)], [(50, 45, 45)])
	return image


def main():
	TARGET.mkdir(parents=True, exist_ok=True)
	write_png(TARGET / "bird.png", bird())
	write_png(TARGET / "butterfly.png", butterfly())
	write_png(TARGET / "gull.png", gull())


if __name__ == "__main__":
	main()
