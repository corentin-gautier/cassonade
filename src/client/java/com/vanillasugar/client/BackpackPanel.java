package com.vanillasugar.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.gear.BackpackContainer;
import com.vanillasugar.gear.BackpackMenu;
import com.vanillasugar.gear.BackpackTogglePayload;
import com.vanillasugar.gear.GearLayout;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;

/**
 * Draws the gear slots and the backpack panel next to the survival inventory, and lays out the
 * leather tabs on its right edge: the backpack's while it is closed, then the world map's.
 */
public final class BackpackPanel {
	// Leather panel with its slots baked in, see tools/gui_textures.py
	private static final Identifier PANEL = VanillaSugar.id("container/backpack_panel");
	// Slots in the inventory's style, see tools/gui_style.py
	private static final Identifier SLOT = VanillaSugar.id("container/gear_slot");
	private static final int TITLE_COLOR = 0xFFF3DEB8;

	/** Set by the backpack key, so the inventory it opens shows the backpack even over the recipe book. */
	private static boolean openRequested;

	private BackpackPanel() {
	}

	/** Opens the inventory with the backpack panel open. */
	public static void openInventory(Minecraft minecraft) {
		setOpen(true);
		openRequested = true;
		minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
	}

	public static boolean takeOpenRequest() {
		boolean requested = openRequested;
		openRequested = false;
		return requested;
	}

	private static BackpackContainer backpack() {
		Player player = Minecraft.getInstance().player;
		return player == null ? null : ((BackpackMenu) player.inventoryMenu).vanillasugar$backpack();
	}

	public static boolean isWorn() {
		BackpackContainer backpack = backpack();
		return backpack != null && backpack.isWorn();
	}

	public static boolean isOpen() {
		BackpackContainer backpack = backpack();
		return backpack != null && backpack.isOpen();
	}

	public static boolean isMapWorn() {
		Player player = Minecraft.getInstance().player;
		return player != null && PlayerGear.isWearing(player, GearSlot.MAP);
	}

	/** Right edge of everything drawn right of the inventory, from the inventory's left edge. */
	public static int rightEdge() {
		return isOpen() ? GearLayout.PANEL_X + GearLayout.PANEL_WIDTH : GearLayout.INVENTORY_WIDTH;
	}

	public static void setOpen(boolean open) {
		BackpackContainer backpack = backpack();
		if (backpack != null) {
			backpack.setOpen(open);
			ClientPlayNetworking.send(new BackpackTogglePayload(open));
		}
	}

	public static boolean contains(double mouseX, double mouseY, int leftPos, int topPos) {
		int x = leftPos + GearLayout.PANEL_X;
		int y = topPos + GearLayout.PANEL_Y;
		return isOpen() && mouseX >= x && mouseX < x + GearLayout.PANEL_WIDTH && mouseY >= y && mouseY < y + GearLayout.PANEL_HEIGHT;
	}

	public static void extractBackground(GuiGraphicsExtractor graphics, int leftPos, int topPos) {
		for (GearSlot slot : GearSlot.values()) {
			slot(graphics, leftPos + GearLayout.GEAR_X, topPos + GearLayout.gearSlotY(slot.ordinal()));
		}

		if (!isOpen()) {
			return;
		}

		int x = leftPos + GearLayout.PANEL_X;
		int y = topPos + GearLayout.PANEL_Y;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, x, y, GearLayout.PANEL_WIDTH, GearLayout.PANEL_HEIGHT);
		// Named after the worn backpack, so a renamed one shows its name
		Minecraft minecraft = Minecraft.getInstance();
		graphics.text(minecraft.font, PlayerGear.of(minecraft.player).backpack().getHoverName(), x + 8, y + 6, TITLE_COLOR, false);
	}

	private static void slot(GuiGraphicsExtractor graphics, int itemX, int itemY) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, itemX - 1, itemY - 1, 18, 18);
	}
}
