#!/usr/bin/env python3
"""Dresses the recipe book as a book: a stitched leather cover around cream pages, leather-bound tabs,
and the slots, arrows and popups of the pages tinted to sit on paper.

- The search field is written on paper (EditBoxMixin), its frame inked when focused.
- The popup of a recipe's variants, on a right click, is a leaf of paper bound in leather; each
  variant a card, pink when ingredients are missing, framed with the ribbon's red when hovered. The
  sheet detailing the hovered one is bound the same way. The page arrows turn red too when hovered.
- The button opening the book, where it is not an inventory tab, is the bare book.

The cover is drawn with the backpack's leather (tools/gui_textures.py). The magnifier and the
colored parts of the sprites are taken from the vanilla jar and kept as they are.

    python3 tools/recipe_book_textures.py ~/Library/Application\\ Support/minecraft/versions/26.3/26.3.jar
"""

import sys
import zipfile
from pathlib import Path

import gui_theme as theme
from gui_style import ICON_OUTLINE, outlined
from gui_textures import HIGHLIGHT, OUTLINE, SHADOW, frame, leather, shade, stitching
from gui_theme import rgb
from pngio import read_png, write_png

ROOT = Path(__file__).resolve().parent.parent
GUI = ROOT / "src/main/resources/assets/minecraft/textures/gui"
MOD_SPRITES = ROOT / "src/main/resources/assets/vanillasugar/textures/gui/sprites"
VANILLA = "assets/minecraft/textures/gui/"

BOOK_WIDTH, BOOK_HEIGHT = 147, 166
# The pages, inside the cover; vanilla's search box, tabs and recipe grid are laid out on them
PAGES_LEFT, PAGES_TOP, PAGES_RIGHT, PAGES_BOTTOM = 8, 8, 140, 159

# Colors are set in tools/gui_theme.py
COVER = rgb(theme.BOOK_COVER)
PAPER = rgb(theme.PAPER)
PAPER_SHADOW = rgb(theme.PAPER_SHADOW)
PAGE_EDGE = rgb(theme.PAGE_EDGE)
INK = rgb(theme.INK)
RIBBON = rgb(theme.RIBBON)
RIBBON_LIGHT = rgb(theme.RIBBON_LIGHT)
RIBBON_DARK = rgb(theme.RIBBON_DARK)
HOVER = rgb(theme.HOVER)
# Lighter than the page, for what lies on it: the search field and the variants' cards
CARD = rgb("#F8F1DE")
# A card whose ingredients are missing
MISSING = rgb("#EBCDBE")
MISSING_EDGE = rgb("#C49A88")

TINTED_SPRITES = [
	"slot_craftable", "slot_many_craftable", "slot_uncraftable", "slot_many_uncraftable",
	"page_forward", "page_forward_highlighted", "page_backward", "page_backward_highlighted",
	"filter_enabled", "filter_enabled_highlighted", "filter_disabled", "filter_disabled_highlighted",
	"furnace_filter_enabled", "furnace_filter_enabled_highlighted", "furnace_filter_disabled", "furnace_filter_disabled_highlighted",
]


def noise(x, y, seed):
	h = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
	h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
	return ((h ^ (h >> 16)) & 0xFF) / 255 - 0.5


def paper(x, y):
	return shade(PAPER, round(noise(x, y, 3) * 4))


def book(vanilla):
	pixels = leather(BOOK_WIDTH, BOOK_HEIGHT, COVER, seed=166)
	frame(pixels, OUTLINE)
	stitching(pixels, inset=3)
	for y in range(PAGES_TOP, PAGES_BOTTOM + 1):
		for x in range(PAGES_LEFT, PAGES_RIGHT + 1):
			if x == PAGES_LEFT or y == PAGES_TOP:
				color = SHADOW
			elif x == PAGES_LEFT + 1 or y == PAGES_TOP + 1:
				# The cover's shadow on the first page
				color = PAPER_SHADOW
			elif x >= PAGES_RIGHT - 2 or y >= PAGES_BOTTOM - 2:
				# Edges of the pages below, every other line
				color = PAGE_EDGE if (x - PAGES_RIGHT if x >= PAGES_RIGHT - 2 else y - PAGES_BOTTOM) % 2 == 0 else paper(x, y)
			else:
				color = paper(x, y)
			pixels[y][x] = color + (255,)
	# Vanilla's magnifier, left of the search box
	for y in range(PAGES_TOP + 2, PAGES_TOP + 22):
		for x in range(PAGES_LEFT + 2, PAGES_LEFT + 18):
			r, g, b, a = vanilla[y + 1][x + 1]
			if a and (r, g, b) != (0x37, 0x37, 0x37):
				pixels[y][x] = (r, g, b, a)
	texture = [[(0, 0, 0, 0)] * 256 for _ in range(256)]
	for y in range(BOOK_HEIGHT):
		texture[y + 1][1:1 + BOOK_WIDTH] = pixels[y]
	return texture


TAB_HEIGHT = 27
# Where the cover starts in each tab sprite: vanilla draws tabs 30 pixels left of the book, the
# selected one 2 more; drawn wider (RecipeBookTabButtonMixin), it crosses the cover into the pages
COVER_IN_TAB = 30
COVER_IN_SELECTED_TAB = 32
SELECTED_TAB_WIDTH = COVER_IN_SELECTED_TAB + PAGES_LEFT + 2
TAB_BORDER = 3
TAB_FILL = rgb(theme.BOOK_TAB)


def tab(selected):
	"""A leather-bound tab. The selected one holds a page: its paper runs across the cover into the
	book's pages, so it is glued to them; the others are brown and stop against the cover."""
	width = SELECTED_TAB_WIDTH if selected else 35
	pages_start = COVER_IN_SELECTED_TAB + PAGES_LEFT if selected else width
	pixels = leather(width, TAB_HEIGHT, COVER, seed=27)
	for y in range(TAB_HEIGHT):
		for x in range(width):
			if x == 0 and y in (0, TAB_HEIGHT - 1):
				pixels[y][x] = (0, 0, 0, 0)
			elif x >= pages_start:
				pixels[y][x] = paper(x, y) + (255,)
			elif x == 0 or y in (0, TAB_HEIGHT - 1):
				pixels[y][x] = OUTLINE + (255,)
			elif x < TAB_BORDER or y < TAB_BORDER or y >= TAB_HEIGHT - TAB_BORDER:
				if x == 1 or y == 1:
					pixels[y][x] = HIGHLIGHT + (255,)
				elif y == TAB_HEIGHT - 2:
					pixels[y][x] = SHADOW + (255,)
			elif selected:
				pixels[y][x] = paper(x, y) + (255,)
			else:
				pixels[y][x] = shade(TAB_FILL, round(noise(x, y, 5) * 4)) + (255,)
	return pixels


def ribbon(pixels):
	"""Vanilla's light blue of a hovered arrow becomes the ribbon's red, as on the variants' cards."""
	def red(p):
		r, g, b, a = p
		if not a or b <= r + 20:
			return p
		return (RIBBON_DARK if b < 0x60 else RIBBON if b < 0xC0 else RIBBON_LIGHT) + (a,)
	return [[red(p) for p in row] for row in pixels]


def tint(pixels):
	"""Greys turn to light shades of paper and black to ink; colored pixels (icons, the red of missing
	ingredients) stay."""
	def warm(p):
		if not p[3] or not p[0] == p[1] == p[2]:
			return p
		if p[0] < 0x20:
			return INK + (p[3],)
		return tuple(round(c * (0.55 + 0.45 * p[0] / 255)) for c in PAPER) + (p[3],)
	return [[warm(p) for p in row] for row in pixels]


def rgba(color):
	return color + (255,)


def search_field(focused):
	"""Nine-slice, two pixels: a pale field on the page with a shadow inside, its frame inked when focused."""
	width, height = 32, 14
	pixels = [[(0, 0, 0, 0)] * width for _ in range(height)]
	for y in range(height):
		for x in range(width):
			ring = min(x, y, width - 1 - x, height - 1 - y)
			if ring == 0:
				color = INK if focused else PAGE_EDGE
			elif ring == 1 and (x == 1 or y == 1):
				color = PAPER_SHADOW
			else:
				color = CARD
			pixels[y][x] = rgba(color)
	return pixels


OVERLAY_SIZE = 32


def overlay():
	"""Nine-slice, four pixels: leather bound around a leaf of paper, a shadow along its top and left."""
	pixels = leather(OVERLAY_SIZE, OVERLAY_SIZE, COVER, seed=32)
	frame(pixels, OUTLINE)
	for y in range(3, OVERLAY_SIZE - 3):
		for x in range(3, OVERLAY_SIZE - 3):
			color = PAPER_SHADOW if x == 3 or y == 3 else paper(x, y)
			pixels[y][x] = rgba(color)
	return pixels


CARD_SIZE = 24


def card(craftable, highlighted):
	"""A variant's card, with vanilla's cut corners, showing what it makes (OverlayRecipeButtonMixin);
	its grid is detailed beside the popup while hovered (RecipeSheet)."""
	fill = CARD if craftable else MISSING
	edge = PAGE_EDGE if craftable else MISSING_EDGE
	pixels = [[(0, 0, 0, 0)] * CARD_SIZE for _ in range(CARD_SIZE)]
	last = CARD_SIZE - 1
	for y in range(CARD_SIZE):
		for x in range(CARD_SIZE):
			ring = min(x, y, last - x, last - y)
			corner = min(x, last - x) + min(y, last - y)
			if corner < 2:
				continue
			if ring == 0 or corner == 2:
				color = RIBBON if highlighted else INK
			elif highlighted and ring == 1:
				color = RIBBON_LIGHT
			elif ring == 1:
				color = shade(fill, 8) if x == 1 or y == 1 else edge
			else:
				color = fill
			pixels[y][x] = rgba(color)
	return pixels


BUTTON_WIDTH, BUTTON_HEIGHT = 20, 18


def book_button(item, highlighted):
	"""The book alone, without the button around it, as on the inventory's tab; its outline lights up
	when hovered."""
	icon = outlined(item)
	pixels = [[(0, 0, 0, 0)] * BUTTON_WIDTH for _ in range(BUTTON_HEIGHT)]
	left, top = (BUTTON_WIDTH - len(icon[0])) // 2, (BUTTON_HEIGHT - len(icon)) // 2
	for y, row in enumerate(icon):
		for x, p in enumerate(row):
			if highlighted and p[:3] == ICON_OUTLINE:
				p = rgba(HOVER)
			pixels[top + y][left + x] = p
	return pixels


def main(jar_path):
	(GUI / "sprites/recipe_book").mkdir(parents=True, exist_ok=True)
	with zipfile.ZipFile(jar_path) as jar:
		write_png(GUI / "recipe_book.png", book(read_png(jar.read(VANILLA + "recipe_book.png"))))
		for name in TINTED_SPRITES:
			path = "sprites/recipe_book/" + name + ".png"
			pixels = tint(read_png(jar.read(VANILLA + path)))
			write_png(GUI / path, ribbon(pixels) if name.startswith("page_") else pixels)
			mcmeta = VANILLA + path + ".mcmeta"
			if mcmeta in jar.namelist():
				(GUI / (path + ".mcmeta")).write_bytes(jar.read(mcmeta))
		knowledge_book = read_png(jar.read("assets/minecraft/textures/item/knowledge_book.png"))
		write_png(GUI / "sprites/recipe_book/button.png", book_button(knowledge_book, False))
		write_png(GUI / "sprites/recipe_book/button_highlighted.png", book_button(knowledge_book, True))
	write_png(GUI / "sprites/recipe_book/overlay_recipe.png", overlay())
	for kind in ("crafting", "furnace"):
		for craftable in (True, False):
			for highlighted in (False, True):
				name = f"{kind}_overlay" + ("" if craftable else "_disabled") + ("_highlighted" if highlighted else "")
				write_png(GUI / f"sprites/recipe_book/{name}.png", card(craftable, highlighted))
	for focused in (False, True):
		name = "paper_text_field" + ("_highlighted" if focused else "")
		write_png(MOD_SPRITES / f"widget/{name}.png", search_field(focused))
	write_png(GUI / "sprites/recipe_book/tab.png", tab(False))
	write_png(GUI / "sprites/recipe_book/tab_selected.png", tab(True))
	print("recipe book written")


if __name__ == "__main__":
	if len(sys.argv) != 2:
		sys.exit(__doc__)
	main(sys.argv[1])
