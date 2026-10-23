"""The colors of every GUI texture the mod draws, in one place.

Change a value here, then regenerate everything with:

    python3 tools/build_gui.py

Colors are "#RRGGBB". Shapes and sizes stay in the scripts that draw them (gui_style.py,
gui_textures.py, recipe_book_textures.py).
"""

# --- Wood: container windows, HUD, widgets, inventory tabs, minimap frame ---------------------------
WOOD = "#6B5744"            # panel
WOOD_LIGHT = "#846E58"      # lit edges of panels and slots
WOOD_DARK = "#4A3B2D"       # shaded edges, unselected tabs
OUTLINE = "#140C07"         # outer outline
SEAM = "#1A120C"            # line between hotbar slots
GROOVE_DARK = "#564535"     # groove carved along the panel's edge
GROOVE_LIGHT = "#7A6550"
FILL = "#2D231D"            # inside of slots
RIM_DARK = "#1C130D"        # slot shadow
RIM_LIGHT = "#3A2D22"
SELECT = "#9C8464"          # selected hotbar slot
SELECT_LIGHT = "#C8B088"    # its frame, outline of hovered tabs
WIDGET_GLARE = "#F0E4CC"    # brightest pixels of buttons and sliders
TOOLTIP = "#201710"         # tooltip background
LABEL = "#E8DABC"           # titles written on the windows ("Inventory", "Chest"...)

# --- HUD -------------------------------------------------------------------------------------------
XP = "#80A642"              # experience bar
XP_LIGHT = "#A6C85C"
XP_DARK = "#58782A"
HEART = "#D43E76"
FILL_BAR = "#C8963C"         # how full a bundle, a shulker box or a backpack is, in its tooltip
FILL_BAR_LIGHT = "#E6BC64"
FILL_BAR_DARK = "#94682A"

# --- Weather icons of the clock HUD ----------------------------------------------------------------
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

# --- Leather: backpack panel, recipe book cover and tabs -------------------------------------------
LEATHER = "#8A562E"         # backpack
BOOK_COVER = "#7A4A26"      # recipe book
LEATHER_OUTLINE = "#2B180C"
LEATHER_HIGHLIGHT = "#B47E4C"
LEATHER_SHADOW = "#5C361A"
STITCH = "#E6C99A"
LEATHER_SLOT_DARK = "#3A2210"
LEATHER_SLOT_LIGHT = "#B07C4C"
LEATHER_SLOT_INNER = "#6B4122"
BOOK_TAB = "#4E301A"        # inside of the unselected recipe book tabs

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
