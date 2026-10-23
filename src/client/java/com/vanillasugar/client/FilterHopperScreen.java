package com.vanillasugar.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.vanillasugar.hopper.FilterHopperMenu;

/**
 * The hopper's window with the filter slot on its left. The slot is cut from the hopper's own texture, so it follows the
 * GUI style (see textures/gui.png) without a texture of its own.
 */
public class FilterHopperScreen extends AbstractContainerScreen<FilterHopperMenu> {
	private static final Identifier HOPPER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/hopper.png");
	// The first slot of the hopper texture, frame included
	private static final int SLOT_U = 43;
	private static final int SLOT_V = 19;
	private static final int SLOT_SIZE = 18;
	private static final int TOOLTIP_WIDTH = 150;

	public FilterHopperScreen(FilterHopperMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 133);
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int xo = (this.width - this.imageWidth) / 2;
		int yo = (this.height - this.imageHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, HOPPER_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, HOPPER_LOCATION, xo + FilterHopperMenu.FILTER_X - 1, yo + SLOT_V, SLOT_U, SLOT_V,
				SLOT_SIZE, SLOT_SIZE, 256, 256);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (this.isHoveringFilter() && !this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
			Component hint = Component.translatable("gui.vanillasugar.filter_hopper.empty");
			graphics.setTooltipForNextFrame(this.font, this.font.split(hint, TOOLTIP_WIDTH), mouseX, mouseY);
		}
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
		List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(stack));
		if (this.isHoveringFilter()) {
			tooltip.add(Component.translatable("gui.vanillasugar.filter_hopper.filter").withStyle(ChatFormatting.GRAY));
		}
		return tooltip;
	}

	private boolean isHoveringFilter() {
		return this.hoveredSlot != null && this.hoveredSlot.index == FilterHopperMenu.FILTER_SLOT;
	}
}
