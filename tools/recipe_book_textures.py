#!/usr/bin/env python3
"""Dresses the recipe book as a book: a stitched leather cover around cream pages, tabs of the cover's
leather, the selected one of paper, and the slots, arrows and popups of the pages tinted to sit on paper.

- The search field is written on paper (EditBoxMixin), its frame inked when focused.
- The popup of a recipe's variants, on a right click, is a plain leaf of paper; each variant shows
  only its item, a faint pink when ingredients are missing, a fine ink frame when hovered. The sheet
  detailing the hovered one is a leaf alike. The page arrows are plain triangles of ink, red when hovered.
- The button opening the book, where it is not an inventory tab, is the bare book.

The cover is drawn with the backpack's leather (tools/gui_textures.py). The magnifier and the
colored parts of the sprites are taken from the vanilla jar and kept as they are.

    python3 tools/recipe_book_textures.py ~/Library/Application\\ Support/minecraft/versions/26.3/26.3.jar
    python3 tools/recipe_book_textures.py --popup-and-tabs    only the popup and the tabs, then gui_atlas.py
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
MOD_SPRITES = ROOT / "src/main/resources/assets/cassonade/textures/gui/sprites"
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
HOVER = rgb(theme.HOVER)
# Lighter than the page, for what lies on it: the search field and the variants' cards
CARD = rgb("#F8F1DE")
# A card whose ingredients are missing
MISSING = rgb("#EED8C8")

TINTED_SPRITES = [
	"slot_craftable", "slot_many_craftable", "slot_uncraftable", "slot_many_uncraftable",
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


# The icon and 3 pixels around it, and one more (RecipeBookComponentMixin): each tab's bottom edge
# hides under the next one's top edge, so the tabs stand glued, one dark line between two. Where the
# cover starts in each tab sprite: the tabs stand out 24 pixels left of the book, all at the same place
# (RecipeBookTabButtonMixin); the selected one, drawn wider, crosses the cover into the pages.
TAB_HEIGHT = 22
COVER_IN_TAB = 24
TAB_WIDTH = COVER_IN_TAB + 5
SELECTED_TAB_WIDTH = COVER_IN_TAB + PAGES_LEFT + 2
TAB_BORDER = 3
# The tabs' icons, engraved: in the leather of the inactive tabs, in ink on the selected one's page
# (RecipeBookTabButtonMixin draws them in place of vanilla's items). X is the cut.
TAB_ICON_SIZE = 16
TAB_ICONS = {
	# Every recipe: a four-pointed star, as a compass rose
	"search": [
		".....X.....",
		".....X.....",
		"....X.X....",
		"....X.X....",
		"..XX...XX..",
		"XX.......XX",
		"..XX...XX..",
		"....X.X....",
		"....X.X....",
		".....X.....",
		".....X.....",
	],
	"equipment": [
		"..........X",
		".........XX",
		"........XX.",
		".......XX..",
		"......XX...",
		".X...XX....",
		"..X.XX.....",
		"...XX......",
		"..X.X......",
		".X...X.....",
		"X..........",
	],
	"blocks": [
		"XXXXXXXXXXX",
		"X....X....X",
		"X....X....X",
		"XXXXXXXXXXX",
		"X..X....X.X",
		"X..X....X.X",
		"XXXXXXXXXXX",
		"X....X....X",
		"X....X....X",
		"XXXXXXXXXXX",
	],
	"ores": [
		"...XXX.....",
		"..X...XX...",
		".X......X..",
		"X...X....X.",
		"X........X.",
		".X...X...X.",
		".X.......X.",
		"..X.....X..",
		"...XXXXX...",
	],
	# Redstone: a lever, its handle leaning on its stone base
	"redstone": [
		"........XX.",
		"........XX.",
		".......X...",
		"......X....",
		".....X.....",
		"....X......",
		"...XXX.....",
		"XXXXXXXXXXX",
		"X.........X",
		"XXXXXXXXXXX",
	],
	"food": [
		".....X.....",
		"......X....",
		"..XXX.XXX..",
		".X...X...X.",
		"X.........X",
		"X.........X",
		"X.........X",
		".X.......X.",
		"..X.....X..",
		"...XX.XX...",
	],
	"misc": [
		"...XXXXX...",
		"..X.....X..",
		".X.......X.",
		"XXXXXXXXXXX",
		"X.........X",
		".X.......X.",
		".X.......X.",
		"..X.....X..",
		"..X.....X..",
		"...XXXXX...",
	],
}


def tab_icon(art, selected):
	"""An icon cut into the tab: the cut, darker, and below and right of it the lit edge of the cut.
	Translucent, so the leather's grain or the page's paper shows through."""
	cut = (0x3A, 0x22, 0x14, 200) if selected else (0x1A, 0x0A, 0x08, 190)
	lit = (0xFF, 0xFF, 0xF4, 150) if selected else (0xF0, 0xC0, 0xB0, 80)
	size = TAB_ICON_SIZE
	pixels = [[(0, 0, 0, 0)] * size for _ in range(size)]
	# Centered with the lit edge, a pixel right and below the cut
	left, top = (size - len(art[0]) - 1) // 2, (size - len(art) - 1) // 2
	marked = {(left + x, top + y) for y, row in enumerate(art) for x, c in enumerate(row) if c == "X"}
	for x, y in marked:
		pixels[y][x] = cut
		if (x + 1, y + 1) not in marked and x + 1 < size and y + 1 < size:
			pixels[y + 1][x + 1] = lit
	return pixels


def tab(selected):
	"""A tab of the cover's leather, running into the cover. The selected one holds a page: its paper
	runs across the cover into the book's pages, so it is the only light one."""
	width = SELECTED_TAB_WIDTH if selected else TAB_WIDTH
	height = TAB_HEIGHT
	pages_start = COVER_IN_TAB + PAGES_LEFT
	pixels = leather(width, height, COVER, seed=27)
	for y in range(height):
		for x in range(width):
			if x == 0 and y in (0, height - 1):
				pixels[y][x] = (0, 0, 0, 0)
			elif selected and x >= pages_start:
				pixels[y][x] = paper(x, y) + (255,)
			elif x == 0 or y in (0, height - 1):
				pixels[y][x] = OUTLINE + (255,)
			elif x == 1 or y == 1:
				pixels[y][x] = HIGHLIGHT + (255,)
			elif y == height - 2:
				pixels[y][x] = SHADOW + (255,)
			elif selected and x >= TAB_BORDER and TAB_BORDER <= y < height - TAB_BORDER:
				pixels[y][x] = paper(x, y) + (255,)
	return pixels


# The page arrows: vanilla's button, 12 by 17, and the triangle inked in its middle
ARROW_WIDTH, ARROW_HEIGHT = 12, 17
ARROW_SIZE = 6


def page_arrow(forward, highlighted):
	"""A plain triangle of ink, pointing to the next or the previous page, as flat as the text beside
	it; red as the ribbon when hovered, like the variants' cards."""
	pixels = [[(0, 0, 0, 0)] * ARROW_WIDTH for _ in range(ARROW_HEIGHT)]
	color = (RIBBON if highlighted else INK) + (255,)
	middle = ARROW_HEIGHT // 2
	left = (ARROW_WIDTH - ARROW_SIZE) // 2
	for y in range(middle - ARROW_SIZE + 1, middle + ARROW_SIZE):
		for x in range(ARROW_SIZE - abs(y - middle)):
			pixels[y][left + x if forward else ARROW_WIDTH - 1 - left - x] = color
	return pixels


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
	"""Nine-slice, four pixels: a plain leaf of paper, edged by a fine dark line and a paler one."""
	pixels = [[(0, 0, 0, 0)] * OVERLAY_SIZE for _ in range(OVERLAY_SIZE)]
	last = OVERLAY_SIZE - 1
	for y in range(OVERLAY_SIZE):
		for x in range(OVERLAY_SIZE):
			ring = min(x, y, last - x, last - y)
			if ring == 0 and min(x, last - x) + min(y, last - y) == 0:
				continue
			pixels[y][x] = rgba(OUTLINE if ring == 0 else PAGE_EDGE if ring == 1 else paper(x, y))
	return pixels


CARD_SIZE = 24


def card(craftable, highlighted):
	"""A variant's card, showing what it makes (OverlayRecipeButtonMixin): nothing around it but the
	paper, a faint pink when ingredients are missing, a fine ink frame with cut corners when hovered.
	Its grid is detailed beside the popup while hovered (RecipeSheet)."""
	pixels = [[(0, 0, 0, 0)] * CARD_SIZE for _ in range(CARD_SIZE)]
	last = CARD_SIZE - 1
	for y in range(CARD_SIZE):
		for x in range(CARD_SIZE):
			ring = min(x, y, last - x, last - y)
			corner = min(x, last - x) + min(y, last - y)
			if corner < 1:
				continue
			if highlighted and ring == 0:
				pixels[y][x] = rgba(INK)
			elif not craftable:
				pixels[y][x] = rgba(MISSING)
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


def popup_and_tabs():
	"""The sprites of the popup of variants and of the tabs alone, without the jar: the others carry
	edits made by hand in textures/gui.png."""
	write_png(GUI / "sprites/recipe_book/overlay_recipe.png", overlay())
	for kind in ("crafting", "furnace"):
		for craftable in (True, False):
			for highlighted in (False, True):
				name = f"{kind}_overlay" + ("" if craftable else "_disabled") + ("_highlighted" if highlighted else "")
				write_png(GUI / f"sprites/recipe_book/{name}.png", card(craftable, highlighted))
	write_png(GUI / "sprites/recipe_book/tab.png", tab(False))
	write_png(GUI / "sprites/recipe_book/tab_selected.png", tab(True))
	(MOD_SPRITES / "recipe_book/tab_icon").mkdir(parents=True, exist_ok=True)
	for name, art in TAB_ICONS.items():
		for selected in (False, True):
			suffix = "_selected" if selected else ""
			write_png(MOD_SPRITES / f"recipe_book/tab_icon/{name}{suffix}.png", tab_icon(art, selected))


def main(jar_path):
	(GUI / "sprites/recipe_book").mkdir(parents=True, exist_ok=True)
	with zipfile.ZipFile(jar_path) as jar:
		write_png(GUI / "recipe_book.png", book(read_png(jar.read(VANILLA + "recipe_book.png"))))
		for name in TINTED_SPRITES:
			path = "sprites/recipe_book/" + name + ".png"
			pixels = tint(read_png(jar.read(VANILLA + path)))
			write_png(GUI / path, pixels)
			mcmeta = VANILLA + path + ".mcmeta"
			if mcmeta in jar.namelist():
				(GUI / (path + ".mcmeta")).write_bytes(jar.read(mcmeta))
		for forward in (True, False):
			for highlighted in (False, True):
				name = "page_" + ("forward" if forward else "backward") + ("_highlighted" if highlighted else "")
				write_png(GUI / f"sprites/recipe_book/{name}.png", page_arrow(forward, highlighted))
		knowledge_book = read_png(jar.read("assets/minecraft/textures/item/knowledge_book.png"))
		write_png(GUI / "sprites/recipe_book/button.png", book_button(knowledge_book, False))
		write_png(GUI / "sprites/recipe_book/button_highlighted.png", book_button(knowledge_book, True))
	for focused in (False, True):
		name = "paper_text_field" + ("_highlighted" if focused else "")
		write_png(MOD_SPRITES / f"widget/{name}.png", search_field(focused))
	popup_and_tabs()
	print("recipe book written")


if __name__ == "__main__":
	if sys.argv[1:] == ["--popup-and-tabs"]:
		popup_and_tabs()
	elif len(sys.argv) == 2:
		main(sys.argv[1])
	else:
		sys.exit(__doc__)
