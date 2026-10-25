package com.cassonade.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import com.cassonade.client.map.SmallText;
import com.cassonade.storage.FoodPreview;

/**
 * A food's tooltip, two named lines of the hunger bar's drumsticks, half ones included: the hunger it
 * restores, then its saturation in gold, how long the bar stays full before it drops. No numbers, as on
 * the HUD.
 */
public class FoodTooltip implements ClientTooltipComponent {
	private static final Identifier FULL = Identifier.withDefaultNamespace("hud/food_full");
	private static final Identifier HALF = Identifier.withDefaultNamespace("hud/food_half");
	private static final Component HUNGER = Component.translatable("gui.cassonade.food.hunger");
	private static final Component SATURATION = Component.translatable("gui.cassonade.food.saturation");
	private static final int ICON = 9;
	/** The hunger bar's spacing, one pixel of overlap. */
	private static final int STEP = 8;
	private static final int ROW_GAP = 1;
	private static final int MARGIN = 2;
	/** Between the names and the drumsticks. */
	private static final int LABEL_GAP = 4;
	private static final int LABEL_COLOR = 0xFFAAAAAA;
	/** The theme's GOLD_LIGHT (tools/gui_theme.py). */
	private static final int SATURATION_TINT = 0xFFFFD76A;

	private final int hunger;
	private final int saturation;

	public FoodTooltip(FoodPreview preview) {
		this.hunger = preview.food().nutrition();
		this.saturation = Mth.ceil(preview.food().saturation());
	}

	private int rows() {
		return saturation > 0 ? 2 : 1;
	}

	private static int labelWidth(Font font) {
		return Math.max(SmallText.width(font, HUNGER), SmallText.width(font, SATURATION));
	}

	@Override
	public int getHeight(Font font) {
		return MARGIN + rows() * (ICON + ROW_GAP) - ROW_GAP + MARGIN;
	}

	@Override
	public int getWidth(Font font) {
		return labelWidth(font) + LABEL_GAP + (Math.max(hunger, saturation) + 1) / 2 * STEP + 1;
	}

	@Override
	public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
		int iconsX = x + labelWidth(font) + LABEL_GAP;
		row(graphics, font, HUNGER, hunger, x, iconsX, y + MARGIN, -1);
		if (rows() == 2) {
			row(graphics, font, SATURATION, saturation, x, iconsX, y + MARGIN + ICON + ROW_GAP, SATURATION_TINT);
		}
	}

	/** The line's name, then its points as drumsticks, two to a drumstick, the last one half when they are odd. */
	private static void row(GuiGraphicsExtractor graphics, Font font, Component name, int points, int x, int iconsX, int y, int tint) {
		SmallText.left(graphics, font, name, x, y + (ICON - SmallText.lineHeight(font)) / 2f, LABEL_COLOR);
		for (int i = 0; i * 2 < points; i++) {
			Identifier sprite = i * 2 + 1 < points ? FULL : HALF;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, iconsX + i * STEP, y, ICON, ICON, tint);
		}
	}
}
