package com.vanillasugar.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Text one GUI scale step smaller than usual, for the minimap's labels. A step, rather than a fixed
 * ratio, keeps each font pixel a whole number of screen pixels, so the text stays sharp.
 */
public final class SmallText {
	private SmallText() {
	}

	public static float scale() {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		return guiScale <= 1 ? 1 : (guiScale - 1f) / guiScale;
	}

	public static int lineHeight(Font font) {
		return (int) Math.ceil(font.lineHeight * scale());
	}

	/** Draws the text centered on x, its top at y. */
	public static void centered(GuiGraphicsExtractor graphics, Font font, Component text, float x, float y, int color) {
		float scale = scale();
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, -font.width(text) / 2, 0, color, true);
		graphics.pose().popMatrix();
	}

}
