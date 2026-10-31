package com.cassonade.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

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

	public static int width(Font font, Component text) {
		return (int) Math.ceil(font.width(text) * scale());
	}

	/** Draws the text centered on x, its top at y. */
	public static void centered(GuiGraphicsExtractor graphics, Font font, Component text, float x, float y, int color) {
		draw(graphics, font, text, x, y, -font.width(text) / 2, color, true);
	}

	/** Draws the text from x, its top at y. */
	public static void left(GuiGraphicsExtractor graphics, Font font, Component text, float x, float y, int color) {
		left(graphics, font, text, x, y, color, true);
	}

	/** Draws the text from x, its top at y, with an explicit shadow choice. */
	public static void left(GuiGraphicsExtractor graphics, Font font, Component text, float x, float y, int color, boolean shadow) {
		draw(graphics, font, text, x, y, 0, color, shadow);
	}

	/** Draws a line already split, from x, its top at y. */
	public static void left(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, float x, float y, int color) {
		float scale = scale();
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, 0, 0, color, true);
		graphics.pose().popMatrix();
	}

	private static void draw(GuiGraphicsExtractor graphics, Font font, Component text, float x, float y, int offset, int color, boolean shadow) {
		float scale = scale();
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, offset, 0, color, shadow);
		graphics.pose().popMatrix();
	}

}
