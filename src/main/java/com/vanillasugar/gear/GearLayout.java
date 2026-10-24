package com.vanillasugar.gear;

/**
 * Where the gear and backpack slots, and the crafting grid they push aside, sit in the player's
 * {@link net.minecraft.world.inventory.InventoryMenu}.
 */
public final class GearLayout {
	// Appended after vanilla's last slot (45, offhand) so vanilla indices are untouched
	public static final int GEAR_START = 46;
	public static final int GEAR_END = GEAR_START + GearSlot.values().length;
	public static final int BACKPACK_START = GEAR_END;
	public static final int BACKPACK_END = BACKPACK_START + BackpackContainer.SIZE;

	// Column above the offhand slot, between the player preview and the crafting grid: compass, map, clock
	public static final int GEAR_X = 77;
	public static final int GEAR_Y = 8;
	public static final int GEAR_COLUMN_SIZE = 3;
	// The slots past the column, the elytra then the backpack, go on along the offhand's row, the armor's bottom one
	public static final int ROW_Y = GEAR_Y + GEAR_COLUMN_SIZE * 18;
	// The gear slots push the crafting grid and its result right of vanilla's spot, as drawn in
	// the inventory texture; the "Crafting" title stays aligned with the grid's left edge, above it
	public static final int CRAFTING_X = 104;
	public static final int CRAFTING_Y = 17;
	public static final int RESULT_X = 152;
	public static final int RESULT_Y = 25;
	public static final int CRAFTING_TITLE_X = CRAFTING_X - 1;
	public static final int CRAFTING_TITLE_Y = CRAFTING_Y - 12;

	// Backpack panel on the left of the inventory, where the recipe book opens
	public static final int INVENTORY_WIDTH = 176;
	public static final int BACKPACK_COLUMNS = 9;
	public static final int PANEL_WIDTH = 7 + BACKPACK_COLUMNS * 18 + 7;
	public static final int PANEL_HEIGHT = 17 + BackpackContainer.SIZE / BACKPACK_COLUMNS * 18 + 7;
	public static final int PANEL_X = -PANEL_WIDTH - 2;
	public static final int PANEL_Y = 0;
	// Tabs toggling the recipe book, the backpack and the world map, along the inventory's top edge;
	// a selected tab reaches two pixels into the panel
	public static final int TAB_WIDTH = 26;
	public static final int TAB_HEIGHT = 24;
	public static final int TAB_X = 4;
	public static final int TAB_Y = 2 - TAB_HEIGHT;
	// The recipe book closes with its ribbon, out of the top of the book, from the book's corner
	public static final int RIBBON_WIDTH = 7;
	public static final int RIBBON_HEIGHT = 18;
	public static final int RIBBON_X = 124;
	public static final int RIBBON_Y = -10;
	// The open backpack closes with its flap's strap, folded over the middle of the panel's top edge
	// and buckled over its title bar
	public static final int STRAP_WIDTH = 14;
	public static final int STRAP_HEIGHT = 20;
	public static final int STRAP_X = PANEL_X + (PANEL_WIDTH - STRAP_WIDTH) / 2;
	public static final int STRAP_Y = PANEL_Y - 8;
	// The inventory moves right by half the panel so both stay centered
	public static final int PANEL_SHIFT = (PANEL_WIDTH + 2) / 2;

	private GearLayout() {
	}

	public static int gearSlotX(GearSlot slot) {
		return slot.ordinal() < GEAR_COLUMN_SIZE ? GEAR_X : GEAR_X + (slot.ordinal() - GEAR_COLUMN_SIZE + 1) * 18;
	}

	public static int gearSlotY(GearSlot slot) {
		return slot.ordinal() < GEAR_COLUMN_SIZE ? GEAR_Y + slot.ordinal() * 18 : ROW_Y;
	}

	public static int tabX(int index) {
		return TAB_X + index * (TAB_WIDTH + 1);
	}

	public static int backpackSlotX(int index) {
		return PANEL_X + 8 + index % BACKPACK_COLUMNS * 18;
	}

	public static int backpackSlotY(int index) {
		return PANEL_Y + 18 + index / BACKPACK_COLUMNS * 18;
	}
}
