package com.vanillasugar.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.gear.GearLayout;

/**
 * Tab on the top edge of the inventory showing an item. A selected tab, the one of the open panel,
 * is lighter and opens into the inventory; clicking it again closes the panel.
 */
public class InventoryTab extends ImageButton {
	private static final WidgetSprites SPRITES = new WidgetSprites(
			VanillaSugar.id("container/inventory_tab"), VanillaSugar.id("container/inventory_tab_highlighted"));
	private static final WidgetSprites SELECTED_SPRITES = new WidgetSprites(
			VanillaSugar.id("container/inventory_tab_selected"), VanillaSugar.id("container/inventory_tab_selected_highlighted"));

	private final ItemStack icon;
	private final boolean selected;

	public InventoryTab(int x, int y, boolean selected, ItemStack icon, Component tooltip, OnPress onPress) {
		super(x, y, GearLayout.TAB_WIDTH, GearLayout.TAB_HEIGHT, selected ? SELECTED_SPRITES : SPRITES, onPress);
		this.icon = icon;
		this.selected = selected;
		setTooltip(Tooltip.create(tooltip));
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractContents(graphics, mouseX, mouseY, a);
		// Centered on the part above the inventory, which starts lower on a tab that is not selected
		graphics.item(icon, getX() + (width - 16) / 2, getY() + (selected ? 3 : 4));
	}
}
