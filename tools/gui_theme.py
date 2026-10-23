"""The colors of every GUI texture the mod draws, in one place.

Change a value here, then regenerate everything with:

    python3 tools/build_gui.py

Colors are "#RRGGBB". Shapes and sizes stay in the scripts that draw them (gui_style.py,
gui_textures.py, recipe_book_textures.py).
"""

# --- Wood: container windows, HUD, widgets, inventory tabs, minimap frame ---------------------------
# A mauve wood, plain: no groove along the panels' edges (GROOVE_* are the panel's color) and no
# lighter rim inside the slots (RIM_LIGHT is their fill)
WOOD = "#674A48"            # panel
WOOD_LIGHT = "#805F5C"      # lit edges of panels and slots
WOOD_DARK = "#473130"       # shaded edges, unselected tabs
OUTLINE = "#130809"         # outer outline
SEAM = "#1A120C"            # line between hotbar slots
GROOVE_DARK = "#674A48"     # groove carved along the panel's edge
GROOVE_LIGHT = "#674A48"
FILL = "#2B1F1F"            # inside of slots
RIM_DARK = "#1B0F0F"        # slot shadow
RIM_LIGHT = "#2B1F1F"
SELECT = "#7FA541"          # what is selected: hotbar slot, beacon power, stonecutter recipe...
SELECT_LIGHT = "#A6C85C"    # light edge of what is selected
WIDGET_GLARE = "#ECD6D0"    # brightest pixels of buttons and sliders
HOVER = "#ECD6D0"           # outline of what is hovered: buttons, tabs, fields, the variants of a recipe
TOOLTIP = "#1F1212"         # tooltip background
LABEL = "#E8DABC"           # titles written on the windows ("Inventory", "Chest"...)

# --- HUD -------------------------------------------------------------------------------------------
XP = "#80A642"              # experience bar
XP_LIGHT = "#A6C85C"
XP_DARK = "#58782A"
HEART = "#D43E76"
FILL_BAR = "#C8963C"         # how full a bundle, a shulker box or a backpack is, in its tooltip
FILL_BAR_LIGHT = "#E6BC64"
FILL_BAR_DARK = "#94682A"

# --- Banners shown when an advancement is made: wood for tasks, gold for goals, purple for challenges
GOLD = "#D8A848"
GOLD_LIGHT = "#F6DA8A"
GOLD_DARK = "#8C6224"
AMETHYST = "#9058C0"
AMETHYST_LIGHT = "#C898EC"
AMETHYST_DARK = "#552C78"

# --- Weather icons of the clock HUD----------------------------------------------------------------
SUN = "#F6CC48"
SUN_RAYS = "#E09A28"
MOON = "#E8E4CC"
MOON_SHADE = "#B8B49C"
CLOUD = "#E4E4E8"
CLOUD_SHADE = "#A8A8B4"
STORM_CLOUD = "#7C7C8C"
STORM_CLOUD_SHADE = "#55555F"
RAIN = "#4C8EE6"
SNOW = "#FFFFFF"
BOLT = "#FFE04A"

# --- Leather: backpack panel, recipe book and journal covers, tabs; oxblood, in tune with the wood ----
LEATHER = "#73413A"         # backpack
BOOK_COVER = "#653732"      # recipe book
LEATHER_OUTLINE = "#241211"
LEATHER_HIGHLIGHT = "#966056"
LEATHER_SHADOW = "#4D2824"
STITCH = "#DFB5A4"
LEATHER_SLOT_DARK = "#301917"
LEATHER_SLOT_LIGHT = "#935F55"
LEATHER_SLOT_INNER = "#59302C"
BOOK_TAB = "#412421"        # inside of the unselected recipe book tabs

# --- World map ------------------------------------------------------------------------------------
FOG = "#181717"             # fog of war over the unexplored chunks
FOG_GRAIN = 6               # how far its pixels stray from it, light grain

# --- Recipe book pages and ribbon ------------------------------------------------------------------
PAPER = "#ECE2C6"
PAPER_SHADOW = "#C9BA96"    # the cover's shadow on the pages
PAGE_EDGE = "#B8A680"       # edges of the pages below
INK = "#3A2A18"             # page number, dark parts of the slots and arrows
RIBBON = "#A82A24"
RIBBON_LIGHT = "#CC3C32"
RIBBON_DARK = "#5A1210"


def rgb(color):
	return tuple(int(color[i:i + 2], 16) for i in (1, 3, 5))
