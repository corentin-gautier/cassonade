"""The colors of every GUI texture the mod draws, in one place.

Change a value here, then regenerate everything with:

    python3 tools/build_gui.py

Colors are "#RRGGBB". Shapes and sizes stay in the scripts that draw them (gui_style.py,
gui_textures.py, recipe_book_textures.py).
"""

# --- Wood: container windows, HUD, widgets, inventory tabs, minimap frame ---------------------------
# A sepia wood: plum in the shadows, brown on the panels, golden on lit edges. Plain: no groove along
# the panels' edges (GROOVE_* are the panel's color) and no lighter rim inside the slots (RIM_LIGHT is
# their fill)
WOOD = "#6E4838"            # panel
WOOD_LIGHT = "#865E46"      # lit edges of panels and slots
WOOD_DARK = "#4D2E28"       # shaded edges, unselected tabs
OUTLINE = "#160608"         # outer outline
SEAM = "#1C1109"            # line around sunk slots: text fields, bundle cells
GROOVE_DARK = "#6E4838"     # groove carved along the panel's edge
GROOVE_LIGHT = "#6E4838"
FILL = "#2E1D1C"            # inside of slots
RIM_DARK = "#1E0D0E"        # slot shadow
RIM_LIGHT = "#2E1D1C"
SELECT = "#7CA72E"          # what is selected: hotbar slot, beacon power, stonecutter recipe...
SELECT_LIGHT = "#A4C94C"    # light edge of what is selected
SELECT_SHADOW = "#0C1601"   # shadow cast inside the selected hotbar slot, see-through
WIDGET_GLARE = "#EBD8C7"    # brightest pixels of buttons and sliders
HOVER = "#EBD8C7"           # outline of what is hovered: buttons, tabs, fields, the variants of a recipe
TOOLTIP = "#231010"         # tooltip background
LABEL = "#ECD9B2"           # titles written on the windows ("Inventory", "Chest"...)

# --- HUD -------------------------------------------------------------------------------------------
XP = "#7DA830"              # experience bar
XP_LIGHT = "#A4C94C"
XP_DARK = "#547A12"
HEART = "#DA3475"
FILL_BAR = "#CE941A"         # how full a bundle, a shulker box or a backpack is, in its tooltip
FILL_BAR_LIGHT = "#F0B935"
FILL_BAR_DARK = "#9B6501"

MOB_HEALTH = "#CD3336"       # health of the mobs in a fight, above their heads
MOB_HEALTH_LIGHT = "#F26456"
MOB_HEALTH_DARK = "#921522"
MOB_HEALTH_TRAIL = "#F2D9C0" # the health just lost, which drains away behind

# --- Banners shown when an advancement is made: wood for tasks, gold for goals, purple for challenges
GOLD = "#DEA62B"
GOLD_LIGHT = "#FFD76A"
GOLD_DARK = "#935F00"
AMETHYST = "#9254C6"
AMETHYST_LIGHT = "#CC94F5"
AMETHYST_DARK = "#58267F"

# --- Weather icons of the clock HUD----------------------------------------------------------------
SUN = "#FFC600"
SUN_RAYS = "#E69700"
MOON = "#EDE3C4"
MOON_SHADE = "#BAB491"
CLOUD = "#E4E4E9"
CLOUD_SHADE = "#A7A7B9"
STORM_CLOUD = "#7B7B93"
STORM_CLOUD_SHADE = "#545463"
RAIN = "#438DEF"
SNOW = "#FFFFFF"
BOLT = "#FFD800"

# --- Leather: backpack panel, recipe book and journal covers, tabs; saddle brown, in tune with the wood ---
LEATHER = "#7C3B27"         # backpack
BOOK_COVER = "#6E311F"      # recipe book
LEATHER_OUTLINE = "#290E0F"
LEATHER_HIGHLIGHT = "#A05B44"
LEATHER_SHADOW = "#552313"
STITCH = "#DDB894"
LEATHER_SLOT_DARK = "#361413"
LEATHER_SLOT_LIGHT = "#9D5A44"
LEATHER_SLOT_INNER = "#622B1A"
BOOK_TAB = "#491F19"        # inside of the unselected recipe book tabs

# --- World map ------------------------------------------------------------------------------------
FOG = "#181717"             # fog of war over the unexplored chunks
FOG_GRAIN = 6               # how far its pixels stray from it, light grain

# --- Container windows' two parts (tools/container_panels.py) --------------------------------------
INVENTORY_PANEL = "#663F31"  # the inventory's part, a little darker than the wood
INVENTORY_SEAM = "#583026"   # its first row, against the block's part
# The block's part of each window, about halfway between the wood and the block's material: a hint of
# it, not a change of style. Light and dark tones follow, as far from it as the wood's from the wood.
PANEL_LEATHER = "#764633"      # crafting table, like the leather on its top
PANEL_SMITHING = "#454351"     # smithing table, its dark iron top
PANEL_BARK = "#4F392C"         # smoker, darkened by smoke
PANEL_STONE = "#5E544F"        # furnace, stonecutter
PANEL_COBBLESTONE = "#5B5552"  # dispenser, dropper
PANEL_PALE_STONE = "#6C625D"   # grindstone
PANEL_DARK_STONE = "#4C4441"   # blast furnace
PANEL_SLATE = "#54525D"        # crafter, brewing stand
PANEL_IRON = "#57545F"         # anvil, hoppers
PANEL_RED_CLOTH = "#822D34"    # enchanting table, the red cloth on its top
PANEL_WOOL = "#70635C"         # loom
PANEL_PAPER = "#776751"        # cartography table
PANEL_SHULKER = "#6A4665"      # shulker boxes
PANEL_ENDER = "#373248"        # ender chest, obsidian

# --- Recipe book pages and ribbon ------------------------------------------------------------------
PAPER = "#F0E1BD"
PAPER_SHADOW = "#CFB984"    # the cover's shadow on the pages
PAGE_EDGE = "#BFA56D"       # edges of the pages below
INK = "#3F280A"             # page number, dark parts of the slots and arrows
RIBBON = "#AD201D"
RIBBON_LIGHT = "#D2322A"
RIBBON_DARK = "#610206"


def rgb(color):
	return tuple(int(color[i:i + 2], 16) for i in (1, 3, 5))
