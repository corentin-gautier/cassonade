#!/usr/bin/env python3
"""The mod's look for the game's GUI: dark brown slots framed in light wood, a little more ornate
than vanilla while staying plain. Everything is drawn from one palette, tools/gui_theme.py:

- HUD: hotbar, selection and offhand slots drawn from scratch; the experience bar as a wooden rod
  bound at its ends and middle; a small crosshair; pink hearts.
- Container windows: vanilla's six greys (panel, highlight, shadow, outline, slot fill, slot shadow)
  mapped to the palette, with a carved groove running along the panel's edge.
- Creative inventory tabs and the inventory's effect boxes: the container greys, unselected tabs in
  dark wood.
- Widgets, tooltips and the HUD's effect boxes: their greys follow a brown ramp by brightness.
- The mod's own gear slots, inventory tabs and minimap frame.
- The hopper's window with its filter slot and a funnel, the vanilla one staying for hopper minecarts.
- Text fields sunk like slots: dark fill, a shadow inside, a light frame when focused.
- What is hovered is outlined in cream, what is selected framed in green like the hotbar's slot: the
  beacon's powers, the enchanting table's options, the stonecutter's recipes, the loom's patterns,
  the cross buttons and the slots under the cursor.

Labels drawn on the panels are made cream by ContainerLabelsMixin.

    python3 tools/gui_style.py ~/Library/Application\\ Support/minecraft/versions/26.3/26.3.jar
"""

import sys
import zipfile
from collections import deque
from pathlib import Path

import gui_theme as theme
from gui_theme import rgb
from pngio import read_png, write_png

ROOT = Path(__file__).resolve().parent.parent
MINECRAFT = ROOT / "src/main/resources/assets/minecraft/textures/gui"
MOD_GUI = ROOT / "src/main/resources/assets/vanillasugar/textures/gui"
MOD_SPRITES = MOD_GUI / "sprites"
VANILLA = "assets/minecraft/textures/gui/"

# Colors are set in tools/gui_theme.py
OUTLINE = rgb(theme.OUTLINE)
SEAM = rgb(theme.SEAM)
WOOD_LIGHT = rgb(theme.WOOD_LIGHT)
WOOD = rgb(theme.WOOD)
WOOD_DARK = rgb(theme.WOOD_DARK)
RIM_DARK = rgb(theme.RIM_DARK)
FILL = rgb(theme.FILL)
RIM_LIGHT = rgb(theme.RIM_LIGHT)
SELECT_LIGHT = rgb(theme.SELECT_LIGHT)
SELECT = rgb(theme.SELECT)
GROOVE_DARK = rgb(theme.GROOVE_DARK)
GROOVE_LIGHT = rgb(theme.GROOVE_LIGHT)
XP_LIGHT = rgb(theme.XP_LIGHT)
XP = rgb(theme.XP)
XP_DARK = rgb(theme.XP_DARK)
FILL_BAR = rgb(theme.FILL_BAR)
FILL_BAR_LIGHT = rgb(theme.FILL_BAR_LIGHT)
FILL_BAR_DARK = rgb(theme.FILL_BAR_DARK)
HEART = rgb(theme.HEART)
WIDGET_GLARE = rgb(theme.WIDGET_GLARE)
HOVER = rgb(theme.HOVER)
TOOLTIP = rgb(theme.TOOLTIP)

# Vanilla container greys and what they become
CONTAINER_GREYS = {
	0xC6: WOOD,         # panel
	0xFF: WOOD_LIGHT,   # highlight, of panels and of slots
	0x55: WOOD_DARK,    # panel shadow
	0x00: OUTLINE,
	0x8B: FILL,         # slot fill
	0x37: RIM_DARK,     # slot shadow
}
# The hopper's filter slot, left of its five slots (HopperFilterMenu.FILTER_X - 1), and the funnel engraved between
HOPPER_SLOT = (43, 19)
HOPPER_FILTER_SLOT = (7, 19)
HOPPER_FUNNEL = [
	"YYYYYYYYY",
	"YYYYYYYYY",
	".YYYYYYY.",
	"..YYYYY..",
	"...YYY...",
	"...YYY...",
	"...YYY...",
	"...YYY...",
	"...YYY...",
]
# How dark the funnel is engraved, as the storage buttons' icons (tools/gui_textures.py)
ENGRAVING = 120 / 255
# Brightness ramp for widgets: vanilla grey level -> brown
RAMP = [(0x00, OUTLINE), (0x30, RIM_DARK), (0x50, FILL), (0x6F, WOOD), (0xA0, WOOD_LIGHT), (0xFF, WIDGET_GLARE)]

CONTAINERS = ["anvil", "beacon", "blast_furnace", "brewing_stand", "cartography_table", "crafter", "crafting_table",
	"dispenser", "enchanting_table", "furnace", "generic_54", "grindstone", "hopper", "horse", "inventory", "loom",
	"nautilus", "shulker_box", "smithing", "smoker", "stonecutter", "villager",
	"creative_inventory/tab_items", "creative_inventory/tab_item_search", "creative_inventory/tab_inventory"]
RAMPED_SPRITES = ["widget/button", "widget/button_highlighted", "widget/button_disabled", "widget/slider",
	"widget/slider_highlighted", "widget/slider_handle", "widget/slider_handle_highlighted", "widget/checkbox",
	"widget/checkbox_highlighted", "widget/checkbox_selected", "widget/checkbox_selected_highlighted", "widget/tab",
	"widget/tab_highlighted", "widget/tab_selected", "widget/tab_selected_highlighted", "widget/scroller",
	"widget/scroller_background", "widget/slot_frame", "widget/cross_button", "widget/cross_button_highlighted",
	"widget/page_forward", "widget/page_forward_highlighted", "widget/page_backward", "widget/page_backward_highlighted",
	"hud/effect_background", "hud/effect_background_ambient"]
# Sprites drawn with the container greys, where the slot grey is an unselected tab or a scroller's
# shadow rather than a slot: it turns to dark wood instead of the slot fill
PANEL_SPRITES = ["container/creative_inventory/scroller", "container/creative_inventory/scroller_disabled",
	"container/inventory/effect_background", "container/inventory/effect_background_ambient"] + [
	f"container/creative_inventory/tab_{side}_{state}_{i}" for side in ("top", "bottom") for state in ("selected", "unselected")
	for i in range(1, 8)]
PANEL_SPRITE_GREYS = {**CONTAINER_GREYS, 0x8B: WOOD_DARK}
HEARTS = ["full", "full_blinking", "half", "half_blinking", "hardcore_full", "hardcore_full_blinking", "hardcore_half",
	"hardcore_half_blinking"]


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


def slot_box(pixels, left, top, width, height, seed=0, fill=None):
	"""A dark slot framed in wood: seam, lit bevel (dark at the bottom right), inner rim, then the fill."""
	fill = fill or FILL
	for y in range(height):
		for x in range(width):
			ring = min(x, y, width - 1 - x, height - 1 - y)
			# Top and left edges of each ring are lit, bottom and right ones in shadow
			lit = y == ring and x < width - 1 - ring or x == ring and y < height - 1 - ring
			if ring == 0:
				color = SEAM
			elif ring == 1:
				color = WOOD_LIGHT if lit else WOOD_DARK
			elif ring == 2:
				color = RIM_DARK if lit else RIM_LIGHT
			else:
				color = shade(fill, noise(left + x, top + y, seed) * 3)
			pixels[top + y][left + x] = rgba(color)


def hotbar():
	pixels = blank(182, 22)
	for y in range(22):
		for x in range(182):
			if x in (0, 181) or y in (0, 21):
				pixels[y][x] = rgba(OUTLINE)
	for i in range(9):
		slot_box(pixels, 1 + 20 * i, 1, 20, 20, seed=i)
	return pixels


def selection():
	"""Thick light frame around the selected slot, its inside left clear."""
	width, height = 24, 23
	pixels = blank(width, height)
	for y in range(height):
		for x in range(width):
			ring = min(x, y, width - 1 - x, height - 1 - y)
			if ring == 0:
				pixels[y][x] = rgba(OUTLINE)
			elif ring == 1:
				pixels[y][x] = rgba(SELECT_LIGHT if x + y < width - 1 and min(x, y) == 1 else SELECT)
			elif ring == 2:
				pixels[y][x] = rgba(SELECT if min(x, y) == 2 and x + y < width else WOOD_LIGHT)
			elif ring == 3:
				pixels[y][x] = rgba(OUTLINE, 160)
	return pixels


def offhand(vanilla):
	"""The offhand slot, at the place vanilla draws it inside the sprite."""
	xs = [x for row in vanilla for x, p in enumerate(row) if p[3]]
	ys = [y for y, row in enumerate(vanilla) for p in row if p[3]]
	left, top, right, bottom = min(xs), min(ys), max(xs), max(ys)
	pixels = blank(len(vanilla[0]), len(vanilla))
	for y in range(top, bottom + 1):
		for x in range(left, right + 1):
			pixels[y][x] = rgba(OUTLINE)
	slot_box(pixels, left + 1, top + 1, right - left - 1, bottom - top - 1, seed=9)
	return pixels


BANDS = (2, 90, 178)


def experience_bar(light, middle, dark, band, width=182, bands=BANDS):
	"""A rod bound with darker bands at its ends and its middle."""
	pixels = blank(width, 5)
	for x in range(width):
		bound = any(b <= x <= b + 1 for b in bands)
		for y in range(5):
			if y in (0, 4) or x in (0, width - 1):
				color = OUTLINE
			elif bound:
				color = band if y != 1 else shade(band, 18)
			else:
				color = (light, middle, dark)[y - 1]
				color = shade(color, noise(x // 3, y, 4) * 4)
			pixels[y][x] = rgba(color)
	return pixels


FILL_BAR_WIDTH = 9 * 18
FILL_BAR_BANDS = (2, FILL_BAR_WIDTH - 4)


def fill_bar_background(bar):
	"""The experience bar's rod at the width of the tooltips' grid, bound at its ends only."""
	def column(x):
		if x < 12:
			return x
		if x >= FILL_BAR_WIDTH - 12:
			return len(bar[0]) - (FILL_BAR_WIDTH - x)
		return 20 + x % 40
	return [[row[column(x)] for x in range(FILL_BAR_WIDTH)] for row in bar]


def fill_bar_progress():
	return experience_bar(FILL_BAR_LIGHT, FILL_BAR, FILL_BAR_DARK, FILL_BAR_DARK, FILL_BAR_WIDTH, FILL_BAR_BANDS)


def inventory_slot(inventory):
	"""One slot of the inventory's grid, cut from its window: side by side, they make the same grid."""
	return [row[7:25] for row in inventory[83:101]]


def crosshair():
	pixels = blank(15, 15)
	for x, y in ((7, 6), (6, 7), (7, 7), (8, 7), (7, 8)):
		pixels[y][x] = (255, 255, 255, 255)
	return pixels


def heart(vanilla):
	"""Vanilla's reds turned pink, keeping how light each pixel is."""
	def pink(p):
		r, g, b, a = p
		if not a or r < g + 40:
			return p
		scale = r / 255
		lift = g / 255
		return tuple(round(c * scale + (255 - c * scale) * lift) for c in HEART) + (a,)
	return [[pink(p) for p in row] for row in vanilla]


def mapped(vanilla, greys):
	return [[rgba(greys[p[0]], p[3]) if p[3] and p[0] == p[1] == p[2] and p[0] in greys else p for p in row] for row in vanilla]


def container(vanilla):
	pixels = mapped(vanilla, CONTAINER_GREYS)
	groove(vanilla, pixels)
	return pixels


def filter_hopper(hopper):
	"""The styled hopper window with a copy of its first slot as the filter, and the funnel centered between them."""
	pixels = [list(row) for row in hopper]
	(sx, sy), (fx, fy) = HOPPER_SLOT, HOPPER_FILTER_SLOT
	for y in range(18):
		pixels[fy + y][fx:fx + 18] = hopper[sy + y][sx:sx + 18]
	size = len(HOPPER_FUNNEL)
	left = fx + 18 + (sx - fx - 18 - size) // 2
	top = fy + (18 - size) // 2
	for y, row in enumerate(HOPPER_FUNNEL):
		for x, letter in enumerate(row):
			if letter == "Y":
				p = pixels[top + y][left + x]
				pixels[top + y][left + x] = rgba(shade_by(p, 1 - ENGRAVING), p[3])
	return pixels


def shade_by(pixel, factor):
	return tuple(round(c * factor) for c in pixel[:3])


def groove(vanilla, pixels):
	"""A carved line following the panel's edge, four pixels in: dark, with a light line inside it."""
	height, width = len(vanilla), len(vanilla[0])
	distance = [[-1] * width for _ in range(height)]
	queue = deque()
	for y in range(height):
		for x in range(width):
			if not vanilla[y][x][3] or vanilla[y][x][:3] == (0, 0, 0):
				distance[y][x] = 0
				queue.append((x, y))
	while queue:
		x, y = queue.popleft()
		for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
			nx, ny = x + dx, y + dy
			if 0 <= nx < width and 0 <= ny < height and distance[ny][nx] < 0:
				distance[ny][nx] = distance[y][x] + 1
				queue.append((nx, ny))
	for y in range(height):
		for x in range(width):
			if vanilla[y][x][:3] == (0xC6, 0xC6, 0xC6) and vanilla[y][x][3]:
				if distance[y][x] == 4:
					pixels[y][x] = rgba(GROOVE_DARK)
				elif distance[y][x] == 5:
					pixels[y][x] = rgba(GROOVE_LIGHT)


def ramp(level):
	for (a, ca), (b, cb) in zip(RAMP, RAMP[1:]):
		if a <= level <= b:
			t = (level - a) / (b - a)
			return tuple(round(x + (y - x) * t) for x, y in zip(ca, cb))
	return RAMP[-1][1]


def ramped(vanilla):
	return [[rgba(ramp(p[0]), p[3]) if p[3] and abs(p[0] - p[1]) < 6 and abs(p[1] - p[2]) < 6 else p for p in row] for row in vanilla]


def tooltip_background(vanilla):
	# Opaque, like the windows
	return [[TOOLTIP + (255,) if p[3] else p for p in row] for row in vanilla]


def tooltip_frame(vanilla):
	"""Vanilla's purple gradient, light at the top, becomes wood."""
	def wood(p):
		if not p[3]:
			return p
		t = max(0, min(1, (p[2] - 127) / 128))
		return tuple(round(d + (l - d) * t) for d, l in zip(WOOD_DARK, WOOD_LIGHT)) + (255,)
	return [[wood(p) for p in row] for row in vanilla]


TEXT_FIELD_WIDTH, TEXT_FIELD_HEIGHT = 200, 20
# Nine-slice border of the text fields, written over vanilla's one pixel
TEXT_FIELD_MCMETA = """{
    "gui": {
        "scaling": {
            "type": "nine_slice",
            "width": 200,
            "height": 20,
            "border": 2
        }
    }
}
"""


def text_field(focused):
	"""The text fields of every screen: sunk like a slot, the frame lighting up when focused."""
	pixels = blank(TEXT_FIELD_WIDTH, TEXT_FIELD_HEIGHT)
	for y in range(TEXT_FIELD_HEIGHT):
		for x in range(TEXT_FIELD_WIDTH):
			ring = min(x, y, TEXT_FIELD_WIDTH - 1 - x, TEXT_FIELD_HEIGHT - 1 - y)
			if ring == 0:
				color = HOVER if focused else WOOD_LIGHT
			elif ring == 1:
				color = RIM_DARK if x == 1 or y == 1 else RIM_LIGHT
			else:
				color = FILL
			pixels[y][x] = rgba(color)
	return pixels


def anvil_text_field(enabled):
	"""The anvil's name field, a long slot; lighter while there is nothing to rename."""
	pixels = blank(110, 16)
	slot_box(pixels, 0, 0, 110, 16, seed=110, fill=FILL if enabled else WOOD_DARK)
	return pixels


def tile(width, height, state):
	"""A wooden tile to click, lit at its top left: outlined in cream when hovered, pressed in and
	framed in green when selected, dark and flat when disabled."""
	pixels = blank(width, height)
	for y in range(height):
		for x in range(width):
			ring = min(x, y, width - 1 - x, height - 1 - y)
			lit = x == ring or y == ring
			if ring == 0:
				color = HOVER if state == "highlighted" else OUTLINE
			elif state == "selected":
				color = SELECT if ring == 1 else RIM_DARK if ring == 2 and lit else FILL
			elif state == "disabled":
				color = WOOD_DARK
			elif ring == 1:
				color = WOOD_LIGHT if lit else WOOD_DARK
			else:
				color = shade(WOOD, 6) if state == "highlighted" else WOOD
			pixels[y][x] = rgba(color)
	return pixels


# Vanilla sprites drawn as tiles, and their sizes
TILES = {
	"container/beacon/button": (22, 22, ("", "highlighted", "selected", "disabled")),
	"container/enchanting_table/enchantment_slot": (108, 19, ("", "highlighted", "disabled")),
	"container/stonecutter/recipe": (16, 18, ("", "highlighted", "selected")),
	"container/loom/pattern": (14, 14, ("", "highlighted", "selected")),
}


def cross_button(vanilla, highlighted):
	"""The small close button: a cream cross on dark wood, outlined in cream when hovered."""
	pixels = blank(len(vanilla[0]), len(vanilla))
	for y, row in enumerate(vanilla):
		for x, p in enumerate(row):
			if not p[3]:
				continue
			ring = min(x, y, len(row) - 1 - x, len(vanilla) - 1 - y)
			if ring == 0:
				color = HOVER if highlighted else OUTLINE
			elif p[0] > 0xB0:
				color = HOVER if highlighted else WIDGET_GLARE
			else:
				color = WOOD if highlighted else FILL
			pixels[y][x] = rgba(color)
	return pixels


def hover_tint(vanilla):
	"""Vanilla's white over a hovered slot, in the cream of what is hovered."""
	return [[rgba(HOVER, p[3]) if p[3] else p for p in row] for row in vanilla]


def gear_slot():
	pixels = blank(18, 18)
	slot_box(pixels, 0, 0, 18, 18, seed=18)
	# The seam of a container slot is the panel around it
	for y in range(18):
		for x in range(18):
			if min(x, y, 17 - x, 17 - y) == 0:
				pixels[y][x] = rgba(RIM_DARK if x + y < 17 and min(x, y) == 0 else WOOD_LIGHT)
	return pixels


def bundle_slot(selected):
	"""A cell of the bundle's tooltip grid, also used for shulker boxes and backpacks: the inventory's
	slot at the cell's size, so the cells make a grid like a chest's. The selected one is lighter."""
	pixels = blank(24, 24)
	slot_box(pixels, 0, 0, 24, 24, seed=24, fill=WOOD_DARK if selected else FILL)
	return pixels


TAB_WIDTH, TAB_HEIGHT = 26, 24


def top_tab(selected, highlighted):
	"""Tab on the top edge of the inventory. Selected, it reaches two pixels into the panel and covers its
	edge, opening into it; otherwise it is shorter, darker and sits on that edge."""
	pixels = blank(TAB_WIDTH, TAB_HEIGHT)
	top, bottom = (0, TAB_HEIGHT) if selected else (2, TAB_HEIGHT - 2)
	fill = WOOD if selected else WOOD_DARK
	if highlighted:
		fill = shade(fill, 12)
	for y in range(top, bottom):
		for x in range(TAB_WIDTH):
			edge_x = x in (0, TAB_WIDTH - 1)
			if edge_x and y == top:
				continue
			if edge_x or y == top:
				color = HOVER if highlighted else OUTLINE
			elif x == 1 or y == top + 1:
				color = shade(fill, 22)
			elif x == TAB_WIDTH - 2:
				color = shade(fill, -16)
			else:
				color = shade(fill, noise(x, y, 26) * 4)
			pixels[y][x] = rgba(color)
	return pixels


TAB_ICON_SIZE = 12


def small_icon(item):
	"""A 16 pixel item drawn on 12, keeping its outline: for the world map tab, where the full size
	map reaches the tab's edges."""
	columns = [round(i * 15 / (TAB_ICON_SIZE - 1)) for i in range(TAB_ICON_SIZE)]
	return [[item[y][x] for x in columns] for y in columns]


# The backpack item's outline (textures/item/backpack.png), around the other tab icons so they read
# as well on the tabs' brown
ICON_OUTLINE = (0x26, 0x11, 0x08)


def outlined(icon):
	"""The icon cropped to its pixels, in a one pixel dark outline."""
	rows = [y for y, row in enumerate(icon) if any(p[3] for p in row)]
	columns = [x for x in range(len(icon[0])) if any(row[x][3] for row in icon)]
	width, height = columns[-1] - columns[0] + 3, rows[-1] - rows[0] + 3
	pixels = blank(width, height)
	for y in range(height - 2):
		for x in range(width - 2):
			pixels[y + 1][x + 1] = icon[rows[0] + y][columns[0] + x]
	for y in range(height):
		for x in range(width):
			if not pixels[y][x][3] and any(0 <= x + dx < width and 0 <= y + dy < height and pixels[y + dy][x + dx][3] == 255
					and pixels[y + dy][x + dx][:3] != ICON_OUTLINE for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
				pixels[y][x] = rgba(ICON_OUTLINE)
	return pixels


def minimap_frame():
	"""Nine-slice wooden frame, four pixels wide, around an empty center; keep in sync with Minimap.BORDER."""
	size = 16
	pixels = blank(size, size)
	for y in range(size):
		for x in range(size):
			ring = min(x, y, size - 1 - x, size - 1 - y)
			lit = x + y < size - 1
			if ring == 0:
				pixels[y][x] = rgba(OUTLINE)
			elif ring == 1:
				pixels[y][x] = rgba(WOOD_LIGHT if lit else WOOD_DARK)
			elif ring == 2:
				pixels[y][x] = rgba(shade(WOOD, noise(x, y, 2) * 6))
			elif ring == 3:
				pixels[y][x] = rgba(OUTLINE)
	return pixels


def main(jar_path):
	with zipfile.ZipFile(jar_path) as jar:
		def vanilla(path):
			return read_png(jar.read(VANILLA + path + ".png"))

		def write(path, pixels):
			target = MINECRAFT / (path + ".png")
			target.parent.mkdir(parents=True, exist_ok=True)
			write_png(target, pixels)
			mcmeta = VANILLA + path + ".png.mcmeta"
			if mcmeta in jar.namelist():
				target.with_name(target.name + ".mcmeta").write_bytes(jar.read(mcmeta))

		write("sprites/hud/hotbar", hotbar())
		write("sprites/hud/hotbar_selection", selection())
		for side in ("left", "right"):
			write(f"sprites/hud/hotbar_offhand_{side}", offhand(vanilla(f"sprites/hud/hotbar_offhand_{side}")))
		background = experience_bar(WOOD_LIGHT, WOOD, WOOD_DARK, RIM_LIGHT)
		progress = experience_bar(XP_LIGHT, XP, XP_DARK, XP_DARK)
		write("sprites/hud/experience_bar_background", background)
		write("sprites/hud/experience_bar_progress", progress)
		write("sprites/hud/crosshair", crosshair())
		for name in HEARTS:
			write(f"sprites/hud/heart/{name}", heart(vanilla(f"sprites/hud/heart/{name}")))
		for name in CONTAINERS:
			write(f"container/{name}", container(vanilla(f"container/{name}")))
		hopper = filter_hopper(container(vanilla("container/hopper")))
		tooltip_slot = inventory_slot(container(vanilla("container/inventory")))
		map_icon = outlined(small_icon(read_png(jar.read("assets/minecraft/textures/item/filled_map.png"))))
		book_icon = outlined(read_png(jar.read("assets/minecraft/textures/item/knowledge_book.png")))
		for name in PANEL_SPRITES:
			write(f"sprites/{name}", mapped(vanilla(f"sprites/{name}"), PANEL_SPRITE_GREYS))
		for name in RAMPED_SPRITES:
			write(f"sprites/{name}", ramped(vanilla(f"sprites/{name}")))
		write("sprites/tooltip/background", tooltip_background(vanilla("sprites/tooltip/background")))
		write("sprites/tooltip/frame", tooltip_frame(vanilla("sprites/tooltip/frame")))
		write("sprites/widget/text_field", text_field(False))
		write("sprites/widget/text_field_highlighted", text_field(True))
		for name in ("text_field", "text_field_highlighted"):
			(MINECRAFT / f"sprites/widget/{name}.png.mcmeta").write_text(TEXT_FIELD_MCMETA)
		write("sprites/container/anvil/text_field", anvil_text_field(True))
		write("sprites/container/anvil/text_field_disabled", anvil_text_field(False))
		for name, (width, height, states) in TILES.items():
			for state in states:
				write(f"sprites/{name}" + (f"_{state}" if state else ""), tile(width, height, state))
		for name in ("slot_highlight_back", "slot_highlight_front"):
			write(f"sprites/container/{name}", hover_tint(vanilla(f"sprites/container/{name}")))
		for highlighted in (False, True):
			write("sprites/widget/cross_button" + ("_highlighted" if highlighted else ""),
				cross_button(vanilla("sprites/widget/cross_button"), highlighted))
		write("sprites/container/bundle/slot_background", bundle_slot(False))
		write("sprites/container/bundle/slot_highlight_back", bundle_slot(True))
	write_png(MOD_GUI / "container/hopper.png", hopper)
	write_png(MOD_SPRITES / "container/gear_slot.png", gear_slot())
	# How full a bundle, a shulker box or a backpack is, in the experience bar's style
	write_png(MOD_SPRITES / "container/fill_bar_background.png", fill_bar_background(background))
	write_png(MOD_SPRITES / "container/fill_bar_progress.png", fill_bar_progress())
	write_png(MOD_SPRITES / "container/tooltip_slot.png", tooltip_slot)
	write_png(MOD_SPRITES / "container/inventory_tab_map.png", map_icon)
	write_png(MOD_SPRITES / "container/inventory_tab_recipe_book.png", book_icon)
	for selected in (False, True):
		for highlighted in (False, True):
			name = "inventory_tab" + ("_selected" if selected else "") + ("_highlighted" if highlighted else "")
			write_png(MOD_SPRITES / f"container/{name}.png", top_tab(selected, highlighted))
	write_png(MOD_SPRITES / "hud/minimap_frame.png", minimap_frame())
	print("GUI style written")


if __name__ == "__main__":
	if len(sys.argv) != 2:
		sys.exit(__doc__)
	main(sys.argv[1])
