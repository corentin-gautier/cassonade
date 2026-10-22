package com.vanillasugar.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.gear.GearLayout;

/** Leather tab stuck to the right edge of the inventory (or of the backpack panel), showing an item. */
public class InventoryTab extends ImageButton {
	private static final WidgetSprites SPRITES = new WidgetSprites(
			VanillaSugar.id("container/backpack_button"), VanillaSugar.id("container/backpack_button_highlighted"));

	private final ItemStack icon;

	public InventoryTab(int x, int y, ItemStack icon, Component tooltip, OnPress onPress) {
		super(x, y, GearLayout.BUTTON_SIZE, GearLayout.BUTTON_SIZE, SPRITES, onPress);
		this.icon = icon;
		setTooltip(Tooltip.create(tooltip));
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractContents(graphics, mouseX, mouseY, a);
		graphics.item(icon, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
	}
}
