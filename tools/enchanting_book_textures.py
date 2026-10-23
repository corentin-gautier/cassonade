#!/usr/bin/env python3
"""The enchanting table's book, drawn in isometric pixel art in place of vanilla's 3D model: a leather
book with gold corners lying on the window, which opens when an item can be enchanted and turns its
pages when the item changes (EnchantmentScreenMixin picks the frames).

The book is modelled as boxes and rendered with a small z-buffer: each face is flat shaded from its
normal and the shade picked from the material's ramp, so the frames stay crisp pixel art.

- open_0 .. open_6: closed, then the cover and its half of the pages rising to lie open.
- page_1 .. page_7: a single page turning from right to left, drawn over the open book.

    python3 tools/enchanting_book_textures.py
"""

import math
from pathlib import Path

import gui_theme as theme
from gui_theme import rgb
from pngio import write_png

SPRITES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/vanillasugar/textures/gui/sprites/container/enchanting_book"

# Keep in sync with EnchantmentScreenMixin
WIDTH, HEIGHT = 36, 32
OPEN_FRAMES = 7
PAGE_FRAMES = 8

# The book, in pixels: a half is HALF wide along x, DEPTH along y; covers one thick, pages three
HALF = 10
DEPTH = 13
COVER = 1
PAGES = 3
# Pages stop short of the covers' edges
INSET = 1
# The open book's right half turns around the spine, along y at the pages' top
SPINE_X = HALF
SPINE_Z = COVER + PAGES
# Where the scene's origin lands in the sprite
ORIGIN_X = DEPTH + 1
ORIGIN_Y = SPINE_Z + HALF + 2

OUTLINE = rgb(theme.LEATHER_OUTLINE)
# Shades of each material, darkest first
LEATHER = [rgb(c) for c in ("#3E1410", "#5E2018", "#7E2E22", "#9E4430")]
GOLD = [rgb(theme.GOLD_DARK), rgb(theme.GOLD), rgb(theme.GOLD_LIGHT), rgb(theme.GOLD_LIGHT)]
PAPER = [rgb(theme.PAGE_EDGE), rgb(theme.PAPER_SHADOW), rgb(theme.PAPER), rgb("#F8F0DA")]
INK = [rgb(theme.AMETHYST_DARK), rgb(theme.AMETHYST_DARK), rgb(theme.AMETHYST), rgb(theme.AMETHYST)]
# Toward the viewer and the light
VIEW = (1, 1, 1)
LIGHT = (0.35, 0.75, 1.0)


def normalize(v):
	length = math.sqrt(sum(c * c for c in v))
	return tuple(c / length for c in v)


def sub(a, b):
	return tuple(x - y for x, y in zip(a, b))


def cross(a, b):
	return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def dot(a, b):
	return sum(x * y for x, y in zip(a, b))


def project(p):
	"""Isometric, two pixels across for one down."""
	x, y, z = p
	return (ORIGIN_X + x - y, ORIGIN_Y + (x + y) / 2 - z)


def turn(p, angle):
	"""Turns a point of the right half around the spine; 0 lies open, 180 closed on the left half."""
	x, y, z = p
	a = math.radians(angle)
	dx, dz = x - SPINE_X, z - SPINE_Z
	return (SPINE_X + dx * math.cos(a) - dz * math.sin(a), y, SPINE_Z + dx * math.sin(a) + dz * math.cos(a))


class Face:
	"""A rectangle from its corner and two edges, painted by a function of where on it a pixel falls."""

	def __init__(self, origin, u, v, paint):
		self.origin, self.u, self.v, self.paint = origin, u, v, paint

	def moved(self, f):
		corner = f(self.origin)
		return Face(corner, sub(f(tuple(a + b for a, b in zip(self.origin, self.u))), corner),
			sub(f(tuple(a + b for a, b in zip(self.origin, self.v))), corner), self.paint)


def box(x0, y0, z0, x1, y1, z1, paint):
	"""The six faces of a box, each painted with paint(face name, x, y, z): where the pixel falls on
	the book laid open, before its right half turns."""
	sx, sy, sz = x1 - x0, y1 - y0, z1 - z0
	faces = {
		"top": ((x0, y0, z1), (sx, 0, 0), (0, sy, 0)),
		"bottom": ((x0, y0, z0), (0, sy, 0), (sx, 0, 0)),
		"east": ((x1, y0, z0), (0, sy, 0), (0, 0, sz)),
		"west": ((x0, y0, z0), (0, 0, sz), (0, sy, 0)),
		"south": ((x0, y1, z0), (0, 0, sz), (sx, 0, 0)),
		"north": ((x0, y0, z0), (sx, 0, 0), (0, 0, sz)),
	}

	def painter(name, o, u, v):
		return lambda s, t: paint(name, *(o[i] + s * u[i] + t * v[i] for i in range(3)))
	return [Face(o, u, v, painter(name, o, u, v)) for name, (o, u, v) in faces.items()]


def cover_paint(face, x, y, z):
	"""Leather; outside, gold corners, a gold band along the spine and a gold diamond on the front."""
	if face == "bottom" and x > HALF:
		corner = (x > 2 * HALF - 2) and (y < 2 or y > DEPTH - 2)
		if corner or HALF + 1 < x < HALF + 2:
			return GOLD, 0
		if abs(x - 1.5 * HALF - 0.5) + abs(y - DEPTH / 2) < 2.5:
			return GOLD, 1
	return LEATHER, 0


def spine_paint(face, x, y, z):
	"""The spine of the closed book, banded in gold."""
	if face == "east" and (abs(z - 1.5) < 0.5 or abs(z - 2 * SPINE_Z + 1.5) < 0.5):
		return GOLD, 0
	return LEATHER, 0


def pages_paint(face, x, y, z):
	"""Cream pages written in purple glyphs, shaded into the fold; their edges striped with the sheets."""
	if face != "top":
		return PAPER, -1 if int(z) % 2 else 0
	if abs(x - HALF) < 1:
		return PAPER, -1
	column = x if x < HALF else x - HALF
	row = int(y)
	if 2 <= column <= HALF - 2 and 1 < y < DEPTH - 1 and row % 2 == 0 and (int(column * 1.7) + row * 3) % 5 != 0:
		return INK, -1
	return PAPER, 0


def sheet_paint(s, t):
	return PAPER, 0


def book(angle):
	"""The faces of the book, its right half turned by the angle; closed, its spine hides the pages."""
	left = box(0, 0, 0, HALF, DEPTH, COVER, cover_paint) + box(INSET, INSET, COVER, HALF, DEPTH - INSET, SPINE_Z, pages_paint)
	right = box(HALF, 0, 0, 2 * HALF, DEPTH, COVER, cover_paint) + box(HALF, INSET, COVER, 2 * HALF - INSET, DEPTH - INSET, SPINE_Z, pages_paint)
	spine = box(HALF, 0, 0, HALF + COVER, DEPTH, 2 * SPINE_Z, spine_paint) if angle == 180 else []
	return left + spine + [face.moved(lambda p: turn(p, angle)) for face in right]


def sheet(angle):
	"""One page, both sides, turned from the right pages over to the left."""
	face = Face((HALF, INSET, SPINE_Z), (HALF - INSET, 0, 0), (0, DEPTH - 2 * INSET, 0), sheet_paint)
	back = Face((HALF, DEPTH - INSET, SPINE_Z), (HALF - INSET, 0, 0), (0, -(DEPTH - 2 * INSET), 0), sheet_paint)
	return [f.moved(lambda p: turn(p, angle)) for f in (face, back)]


def render(faces):
	pixels = [[(0, 0, 0, 0)] * WIDTH for _ in range(HEIGHT)]
	depth = [[-math.inf] * WIDTH for _ in range(HEIGHT)]
	light = normalize(LIGHT)
	for face in faces:
		normal = normalize(cross(face.u, face.v))
		facing = dot(normal, VIEW)
		if facing <= 1e-6:
			continue
		brightness = 0.25 + 0.75 * max(0.0, dot(normal, light))
		o = project(face.origin)
		pu = sub(project(tuple(a + b for a, b in zip(face.origin, face.u))), o)
		pv = sub(project(tuple(a + b for a, b in zip(face.origin, face.v))), o)
		det = pu[0] * pv[1] - pu[1] * pv[0]
		if abs(det) < 1e-6:
			continue
		corners = [o, (o[0] + pu[0], o[1] + pu[1]), (o[0] + pv[0], o[1] + pv[1]), (o[0] + pu[0] + pv[0], o[1] + pu[1] + pv[1])]
		for py in range(max(0, int(min(c[1] for c in corners)) - 1), min(HEIGHT, int(max(c[1] for c in corners)) + 2)):
			for px in range(max(0, int(min(c[0] for c in corners)) - 1), min(WIDTH, int(max(c[0] for c in corners)) + 2)):
				dx, dy = px + 0.5 - o[0], py + 0.5 - o[1]
				s = (dx * pv[1] - dy * pv[0]) / det
				t = (pu[0] * dy - pu[1] * dx) / det
				if not (-1e-6 <= s <= 1 + 1e-6 and -1e-6 <= t <= 1 + 1e-6):
					continue
				point = tuple(face.origin[i] + s * face.u[i] + t * face.v[i] for i in range(3))
				d = dot(point, VIEW)
				if d < depth[py][px] - 1e-6:
					continue
				depth[py][px] = d
				ramp, bias = face.paint(s, t)
				level = max(0, min(3, round(brightness * 3) + bias))
				pixels[py][px] = ramp[level] + (255,)
	# A dark outline around the whole
	outlined = [row[:] for row in pixels]
	for y in range(HEIGHT):
		for x in range(WIDTH):
			if not pixels[y][x][3] and any(0 <= x + dx < WIDTH and 0 <= y + dy < HEIGHT and pixels[y + dy][x + dx][3]
					for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
				outlined[y][x] = OUTLINE + (255,)
	return outlined


def main():
	SPRITES.mkdir(parents=True, exist_ok=True)
	for i in range(OPEN_FRAMES):
		write_png(SPRITES / f"open_{i}.png", render(book(180 * (1 - i / (OPEN_FRAMES - 1)))))
	# Edge on at 45 degrees, the page would vanish: the angles step around it
	for i in range(1, PAGE_FRAMES):
		write_png(SPRITES / f"page_{i}.png", render(sheet(15 + 150 * (i - 1) / (PAGE_FRAMES - 2))))
	print("Enchanting book written")


if __name__ == "__main__":
	main()
