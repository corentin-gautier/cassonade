package com.cassonade.client;

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

import com.cassonade.Cassonade;
import com.cassonade.hopper.HopperFilter;
import com.cassonade.hopper.HopperFilterMenu;

/**
 * The hopper's window with the filter slot on its left and a funnel between them, drawn from the styled vanilla window by
 * tools/gui_style.py (see textures/gui.png). The vanilla one stays as it is for hopper minecarts, which have no filter.
 */
public class HopperFilterScreen extends AbstractContainerScreen<HopperFilterMenu> {
	private static final Identifier HOPPER_LOCATION = Cassonade.id("textures/gui/container/hopper.png");
	private static final int TOOLTIP_WIDTH = 150;

	public HopperFilterScreen(HopperFilterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 133);
		this.inventoryLabelY = this.imageHeight - 94;
	}

	/** Centered as the vanilla hopper's title (HopperScreenMixin). */
	@Override
	protected void init() {
		super.init();
		this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int xo = (this.width - this.imageWidth) / 2;
		int yo = (this.height - this.imageHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, HOPPER_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (this.isHoveringFilter() && !this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
			Component hint = Component.translatable("gui.cassonade.hopper_filter.empty");
			graphics.setTooltipForNextFrame(this.font, this.font.split(hint, TOOLTIP_WIDTH), mouseX, mouseY);
		}
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
		List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(stack));
		if (this.isHoveringFilter()) {
			tooltip.add(Component.translatable(HopperFilter.listsItems(stack) ? "gui.cassonade.hopper_filter.filter_many" : "gui.cassonade.hopper_filter.filter").withStyle(ChatFormatting.GRAY));
		}
		return tooltip;
	}

	private boolean isHoveringFilter() {
		return this.hoveredSlot != null && this.hoveredSlot.index == HopperFilterMenu.FILTER_SLOT;
	}
}
