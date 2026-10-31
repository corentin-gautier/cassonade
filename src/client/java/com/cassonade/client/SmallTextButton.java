package com.cassonade.client;

import com.cassonade.client.map.SmallText;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.network.chat.Component;

/** A discreet text button: small and faded, its clickable area shrunk to match, and fully opaque when pointed at. */
public class SmallTextButton extends PlainTextButton {
	public static final float OPACITY = 0.6f;

	public SmallTextButton(int x, int y, int width, int height, Component message, OnPress onPress, Font font) {
		super(x, y, (int) Math.ceil(width * SmallText.scale()), (int) Math.ceil(height * SmallText.scale()), message, onPress, font);
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		float scale = SmallText.scale();
		graphics.pose().pushMatrix();
		graphics.pose().translate(getX(), getY());
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(-getX(), -getY());
		float fullAlpha = alpha;
		if (!isHoveredOrFocused()) {
			alpha *= OPACITY;
		}
		super.extractContents(graphics, mouseX, mouseY, a);
		alpha = fullAlpha;
		graphics.pose().popMatrix();
	}
}
