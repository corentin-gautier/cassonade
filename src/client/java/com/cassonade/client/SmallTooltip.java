package com.cassonade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A line of small text in the tooltips' frame, as the anvil writes its cost (AnvilScreenMixin): the
 * grindstone's experience and the furnace's times use it too, at the same place in their windows.
 */
public final class SmallTooltip {
	/** From the window's right edge to the text's: the inventory's margin, the tooltip's frame and padding. */
	public static final int RIGHT = 8 + 1 + 6;
	/** The mod's gold for experience levels and figures. */
	public static final int GOLD = 0xFFFFD76A;
	/** Divider centers measured in each vanilla container texture. */
	public static final int FURNACE_DIVIDER = 86;
	public static final int BREWING_STAND_DIVIDER = 90;
	public static final int GRINDSTONE_DIVIDER = 82;
	private static final int CAPITAL_HEIGHT = 7;

	private SmallTooltip() {
	}

	/** The line, its right edge at x, centered on y; returns its left edge. */
	public static int draw(GuiGraphicsExtractor graphics, Font font, Component line, int right, int middle, int color) {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		float scale = Math.max(1, guiScale - 2) / (float) guiScale;
		int width = Mth.ceil(font.width(line) * scale);
		int height = Mth.ceil(CAPITAL_HEIGHT * scale);
		int left = right - width;
		int top = middle - height / 2;
		TooltipRenderUtil.extractTooltipBackground(graphics, left, top, width, height, null);
		graphics.pose().pushMatrix();
		graphics.pose().translate(left, top);
		graphics.pose().scale(scale, scale);
		graphics.text(font, line, 0, 0, color);
		graphics.pose().popMatrix();
		return left;
	}

	/** The line centered on x, and on y. */
	public static void drawCentered(GuiGraphicsExtractor graphics, Font font, Component line, int center, int middle, int color) {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		float scale = Math.max(1, guiScale - 2) / (float) guiScale;
		draw(graphics, font, line, center + Mth.ceil(font.width(line) * scale) / 2, middle, color);
	}

	/** Center the tooltip on its screen's horizontal divider. */
	public static int drawAtDivider(GuiGraphicsExtractor graphics, Font font, Component line, int left, int top, int width, int divider,
			int color) {
		return draw(graphics, font, line, left + width - RIGHT, top + divider, color);
	}
}
