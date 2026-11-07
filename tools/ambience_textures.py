"""Draws the bird, gull, crow, starling, butterfly, robin, squirrel and scorpion (textures/entity), in the box layout of
client/ambience/Birds, Butterflies, Robins, Squirrels and Scorpions. The
butterfly's wings are light greys, tinted by the game to each one's color.

Usage:
    python3 tools/ambience_textures.py, then open the PNGs in src/textures/world.psd (tools/atlas_psd.py)
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


CROW = [(24, 23, 28), (30, 29, 35), (19, 18, 23)]
CROW_SHEEN = [(36, 37, 48), (30, 31, 41)]
CROW_BEAK = [(14, 13, 16)]


def crow():
	"""The bird's boxes, black all over, the back and wings with a little blue: seen from below, nothing paler than the sky's shadow."""
	image = [[(0, 0, 0, 0)] * 64 for _ in range(32)]
	box(image, 0, 0, 4, 3, 8, CROW_SHEEN, CROW, CROW)
	box(image, 0, 12, 3, 3, 3, CROW, CROW, CROW)
	box(image, 12, 12, 1, 1, 2, CROW_BEAK, CROW_BEAK, CROW_BEAK)
	box(image, 0, 19, 4, 1, 4, CROW, CROW, CROW)
	box(image, 24, 0, 6, 1, 5, CROW_SHEEN, CROW, CROW)
	box(image, 24, 7, 6, 1, 4, CROW, CROW, CROW)
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


def starling():
	"""Three frames of a starling seen from afar, one under the other, wings up, level and down: a few dark pixels, as a particle is."""
	frames = [
		"#......#",
		".#....#.",
		"..##+#..",
		"...##...",

		"........",
		".+####+.",
		"##.##.##",
		"...##...",

		"...##...",
		"..##+#..",
		".#.##.#.",
		"#......#",
	]
	colors = {"#": (20, 19, 26, 255), "+": (32, 30, 40, 255), ".": (0, 0, 0, 0)}
	return [[colors[pixel] for pixel in row] for row in frames]


ROBIN_BACK = [(118, 94, 64), (108, 86, 59), (126, 101, 70)]
ROBIN_WING = [(96, 76, 52), (104, 83, 57)]
ROBIN_EDGE = [(74, 58, 41), (80, 63, 44)]
ROBIN_BREAST = [(214, 102, 44), (224, 114, 50), (204, 94, 40)]
ROBIN_BELLY = [(226, 220, 206), (214, 208, 194)]
ROBIN_DARK = [(36, 31, 28)]


def faces(image, u, v, width, height, depth, rows):
	"""Fills a box's faces row by row, top to bottom: rows maps each face (top, bottom, west, north, east, south) to its rows of colors."""
	places = {
		"top": (u + depth, v, width, depth), "bottom": (u + depth + width, v, width, depth),
		"west": (u, v + depth, depth, height), "north": (u + depth, v + depth, width, height),
		"east": (u + depth + width, v + depth, depth, height), "south": (u + 2 * depth + width, v + depth, width, height),
	}
	for face, (x, y, w, h) in places.items():
		colors = rows[face]
		for py in range(h):
			for px in range(w):
				image[y + py][x + px] = (*RANDOM.choice(colors[min(py, len(colors) - 1)]), 255)


def robin():
	"""A robin perched or flying off: brown back and wings, the orange breast up to its face, a pale belly. Its head looks to +Z,
	the south face of each box."""
	image = [[(0, 0, 0, 0)] * 32 for _ in range(32)]
	side = [ROBIN_BACK, ROBIN_BREAST, ROBIN_BELLY]
	faces(image, 0, 0, 3, 3, 4, {"top": [ROBIN_BACK], "bottom": [ROBIN_BELLY, ROBIN_BELLY, ROBIN_BELLY, ROBIN_BREAST], "west": side, "east": side,
			"north": [ROBIN_BACK, ROBIN_BACK, ROBIN_BELLY], "south": [ROBIN_BREAST, ROBIN_BREAST, ROBIN_BELLY]})
	head = [ROBIN_BACK, ROBIN_BREAST, ROBIN_BREAST]
	faces(image, 0, 8, 3, 3, 3, {"top": [ROBIN_BACK], "bottom": [ROBIN_BREAST], "west": head, "east": head,
			"north": [ROBIN_BACK, ROBIN_BACK, ROBIN_BREAST], "south": [ROBIN_BREAST]})
	# An eye in the middle of each side of the head, where the brown cap meets the orange
	image[8 + 3 + 1][1] = (*ROBIN_DARK[0], 255)
	image[8 + 3 + 1][3 + 3 + 1] = (*ROBIN_DARK[0], 255)
	box(image, 12, 8, 1, 1, 1, ROBIN_DARK, ROBIN_DARK, ROBIN_DARK)
	faces(image, 0, 14, 2, 1, 3, {face: [ROBIN_EDGE] for face in ("top", "bottom", "west", "north", "east", "south")})
	wing = [ROBIN_WING, ROBIN_WING, ROBIN_EDGE]
	faces(image, 16, 14, 1, 3, 4, {"top": [ROBIN_BACK], "bottom": [ROBIN_EDGE], "west": wing, "east": wing, "north": wing, "south": wing})
	return image


SQUIRREL_FUR = [(176, 84, 38), (166, 78, 35), (186, 92, 42)]
SQUIRREL_DARK = [(138, 63, 29), (128, 58, 27)]
SQUIRREL_TAIL = [(160, 74, 34), (148, 67, 31), (172, 82, 38), (138, 63, 29)]
SQUIRREL_BELLY = [(232, 222, 200), (222, 212, 190)]
SQUIRREL_EYE = (30, 24, 22)
SQUIRREL_NOSE = [(96, 46, 24)]


def squirrel():
	"""A red squirrel: russet back, head and bushy tail, a cream belly and throat, dark tufts on its ears. Its head looks to +Z,
	the south face of each box."""
	image = [[(0, 0, 0, 0)] * 32 for _ in range(32)]
	fur = [SQUIRREL_FUR]
	side = [SQUIRREL_FUR, SQUIRREL_FUR, SQUIRREL_BELLY]
	faces(image, 0, 0, 3, 3, 5, {"top": fur, "bottom": [SQUIRREL_BELLY], "west": fur, "east": fur, "north": fur,
			"south": [SQUIRREL_FUR, SQUIRREL_BELLY, SQUIRREL_BELLY]})
	faces(image, 0, 8, 3, 3, 3, {"top": fur, "bottom": [SQUIRREL_BELLY], "west": side, "east": side, "north": fur, "south": side})
	# An eye in the middle of each side of the head
	image[8 + 3 + 1][1] = (*SQUIRREL_EYE, 255)
	image[8 + 3 + 1][3 + 3 + 1] = (*SQUIRREL_EYE, 255)
	faces(image, 12, 8, 2, 2, 1, {"top": fur, "bottom": [SQUIRREL_BELLY], "west": [SQUIRREL_FUR, SQUIRREL_BELLY],
			"east": [SQUIRREL_FUR, SQUIRREL_BELLY], "north": fur, "south": [SQUIRREL_FUR, SQUIRREL_NOSE]})
	box(image, 20, 8, 1, 1, 1, SQUIRREL_DARK, SQUIRREL_DARK, SQUIRREL_DARK)
	box(image, 0, 14, 2, 3, 3, SQUIRREL_TAIL, SQUIRREL_TAIL, SQUIRREL_TAIL)
	box(image, 0, 20, 3, 4, 4, SQUIRREL_TAIL, SQUIRREL_TAIL, SQUIRREL_TAIL)
	box(image, 24, 0, 1, 2, 1, SQUIRREL_DARK, SQUIRREL_DARK, SQUIRREL_DARK)
	box(image, 24, 4, 1, 2, 2, SQUIRREL_FUR, SQUIRREL_DARK, SQUIRREL_FUR)
	return image


SCORPION_SHELL = [(206, 170, 104), (198, 162, 98), (214, 178, 112)]
SCORPION_BAND = [(166, 128, 72), (156, 120, 68)]
SCORPION_BELLY = [(226, 202, 146), (218, 194, 138)]
SCORPION_CLAW = [(184, 144, 82), (176, 136, 78)]
SCORPION_STING = [(110, 62, 32), (96, 54, 28)]
SCORPION_EYE = (34, 26, 22)


def scorpion():
	"""A pale desert scorpion, sand-colored with darker bands across its back and tail, a cream belly and legs, an amber sting.
	Its head looks to +Z, the south face of each box."""
	image = [[(0, 0, 0, 0)] * 32 for _ in range(32)]
	shell = [SCORPION_SHELL]
	side = [SCORPION_SHELL, SCORPION_BELLY]
	faces(image, 0, 0, 4, 2, 6, {"top": [SCORPION_BAND, SCORPION_SHELL] * 3, "bottom": [SCORPION_BELLY], "west": side, "east": side,
			"north": side, "south": side})
	# Two eyes on its front, at the top corners: side by side, they read as a mouth
	image[6][6 + 4 + 6] = (*SCORPION_EYE, 255)
	image[6][6 + 4 + 6 + 3] = (*SCORPION_EYE, 255)
	box(image, 20, 0, 3, 1, 1, SCORPION_BELLY, SCORPION_BELLY, SCORPION_BELLY)
	box(image, 20, 2, 1, 1, 3, SCORPION_CLAW, SCORPION_CLAW, SCORPION_CLAW)
	box(image, 0, 8, 2, 1, 2, SCORPION_CLAW, SCORPION_CLAW, SCORPION_CLAW)
	box(image, 8, 8, 1, 1, 2, SCORPION_CLAW, SCORPION_CLAW, SCORPION_CLAW)
	band = [SCORPION_BAND, SCORPION_SHELL]
	faces(image, 0, 11, 2, 2, 2, {"top": band, "bottom": [SCORPION_BELLY], "west": shell, "east": shell, "north": shell, "south": shell})
	box(image, 8, 11, 1, 1, 2, SCORPION_STING, SCORPION_STING, SCORPION_STING)
	return image


def main():
	TARGET.mkdir(parents=True, exist_ok=True)
	write_png(TARGET / "bird.png", bird())
	write_png(TARGET / "butterfly.png", butterfly())
	write_png(TARGET / "gull.png", gull())
	write_png(TARGET / "crow.png", crow())
	write_png(TARGET / "starling.png", starling())
	write_png(TARGET / "robin.png", robin())
	write_png(TARGET / "squirrel.png", squirrel())
	write_png(TARGET / "scorpion.png", scorpion())


if __name__ == "__main__":
	main()
