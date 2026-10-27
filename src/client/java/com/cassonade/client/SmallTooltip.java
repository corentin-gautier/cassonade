package com.cassonade.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import com.cassonade.client.map.SmallText;

/**
 * A line of small text in the tooltips' frame, as the anvil writes its cost (AnvilScreenMixin): the
 * grindstone's experience and the furnace's times use it too, at the same place in their windows.
 */
public final class SmallTooltip {
	/** The middle of the line the anvil writes its cost on, a little lower, from the window's top. */
	public static final int LINE_MIDDLE = 69 + 4 + 3;
	/** From the window's right edge to the text's: the inventory's margin, the tooltip's frame and padding. */
	public static final int RIGHT = 8 + 1 + 6;
	/** The mod's gold for experience levels and figures. */
	public static final int GOLD = 0xFFFFD76A;
	private static final int CAPITAL_HEIGHT = 7;

	private SmallTooltip() {
	}

	/** The line, its right edge at x, centered on y; returns its left edge. */
	public static int draw(GuiGraphicsExtractor graphics, Font font, Component line, int right, int middle, int color) {
		float scale = SmallText.scale();
		int width = SmallText.width(font, line);
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
}
