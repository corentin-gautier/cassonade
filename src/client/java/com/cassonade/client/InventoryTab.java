package com.cassonade.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import com.cassonade.Cassonade;
import com.cassonade.gear.GearLayout;

/**
 * Tab on the top edge of the inventory showing an item, or an icon of the GUI atlas. A selected
 * tab, the one of the open panel, is lighter and opens into the inventory; clicking it again closes
 * the panel.
 */
public final class InventoryTab extends ImageButton {
	private static final WidgetSprites SPRITES = new WidgetSprites(
			Cassonade.id("container/inventory_tab"), Cassonade.id("container/inventory_tab_highlighted"));
	private static final WidgetSprites SELECTED_SPRITES = new WidgetSprites(
			Cassonade.id("container/inventory_tab_selected"), Cassonade.id("container/inventory_tab_selected_highlighted"));

	/** Rows of the tab's inside, under its outline and lit edge and down to the inventory's top. */
	private static final int INSIDE_TOP = 2;
	private static final int INSIDE_TOP_UNSELECTED = 4;
	private static final int INSIDE_BOTTOM = 22;
	private static final int ITEM_SIZE = 16;

	private final @Nullable ItemStack item;
	private final @Nullable Identifier icon;
	private final int iconWidth;
	private final int iconHeight;
	private final boolean selected;

	public InventoryTab(int x, int y, boolean selected, ItemStack item, Component tooltip, OnPress onPress) {
		this(x, y, selected, item, null, ITEM_SIZE, ITEM_SIZE, tooltip, onPress);
	}

	/** A tab showing an icon of the GUI atlas, such as an item redrawn with a darker outline. */
	public InventoryTab(int x, int y, boolean selected, Identifier icon, int iconWidth, int iconHeight, Component tooltip, OnPress onPress) {
		this(x, y, selected, null, icon, iconWidth, iconHeight, tooltip, onPress);
	}

	private InventoryTab(int x, int y, boolean selected, @Nullable ItemStack item, @Nullable Identifier icon, int iconWidth, int iconHeight,
			Component tooltip, OnPress onPress) {
		super(x, y, GearLayout.TAB_WIDTH, GearLayout.TAB_HEIGHT, selected ? SELECTED_SPRITES : SPRITES, onPress);
		this.item = item;
		this.icon = icon;
		this.iconWidth = iconWidth;
		this.iconHeight = iconHeight;
		this.selected = selected;
		setTooltip(Tooltip.create(tooltip));
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractContents(graphics, mouseX, mouseY, a);
		// Centered on the inside above the inventory, which starts lower on a tab that is not selected
		int top = selected ? INSIDE_TOP : INSIDE_TOP_UNSELECTED;
		int x = getX() + (width - iconWidth) / 2;
		int y = getY() + top + (INSIDE_BOTTOM - top - iconHeight) / 2;
		if (item != null) {
			graphics.item(item, x, y);
		} else {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x, y, iconWidth, iconHeight);
		}
	}
}
