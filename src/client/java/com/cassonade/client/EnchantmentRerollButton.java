package com.cassonade.client;

import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * The enchanting table's Reroll, a button of its own under the offers' frame, as wide: drawn as any button, its name in
 * the middle, and its price in levels on the right in the gold of the offers' costs, dimmed when out of reach.
 */
public class EnchantmentRerollButton extends AbstractButton {
	public static final int HEIGHT = 16;
	private static final int COST = 0xFFFFD76A;
	private static final int COST_DISABLED = 0xFF8C605D;
	private static final int COST_RIGHT = 5;

	private final Runnable press;
	private final IntSupplier cost;
	private boolean affordable = true;

	public EnchantmentRerollButton(int x, int y, int width, Runnable press, IntSupplier cost) {
		super(x, y, width, HEIGHT, Component.translatable("gui.cassonade.enchanting.reroll"));
		this.press = press;
		this.cost = cost;
	}

	public void setAffordable(boolean affordable) {
		this.affordable = affordable;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		press.run();
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		extractDefaultSprite(graphics);
		extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
		Font font = Minecraft.getInstance().font;
		String price = String.valueOf(cost.getAsInt());
		graphics.text(font, price, getX() + getWidth() - COST_RIGHT - font.width(price), getY() + (HEIGHT - font.lineHeight) / 2 + 1,
				active && affordable ? COST : COST_DISABLED, true);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
