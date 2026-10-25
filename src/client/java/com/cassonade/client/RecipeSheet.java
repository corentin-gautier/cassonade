package com.cassonade.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import com.cassonade.client.map.SmallText;

/**
 * The detail of a recipe's variant, beside the popup of variants while one is hovered: a leaf like
 * the popup, with what it makes, whether the ingredients are at hand, and the full size grid,
 * an arrow and the result.
 */
public final class RecipeSheet {
	/** The popup's leaf, its border four pixels (tools/recipe_book_textures.py). */
	private static final Identifier SPRITE = Identifier.withDefaultNamespace("recipe_book/overlay_recipe");
	private static final int BORDER = 4;
	private static final int PADDING = 5;
	private static final int CELL = 18;
	private static final int RESULT = 26;
	private static final int GAP = 5;
	private static final int ARROW = 10;
	private static final int GRID = 3;
	/** Ingredients that can be several items show each in turn, as in the book. */
	private static final long CYCLE_MILLIS = 1000;
	// The recipe book's inks (tools/gui_theme.py)
	private static final int INK = 0xFF3A2A18;
	private static final int GREEN_INK = 0xFF3E6A1A;
	private static final int RED_INK = 0xFF8E2418;
	/** The cells, a shade darker than the paper, flat: nothing framed, so the sheet stays quiet. */
	private static final int CELL_FILL = 0xFFE1D5B4;
	private static final Component CRAFTABLE = Component.translatable("gui.cassonade.recipe.craftable");
	private static final Component MISSING = Component.translatable("gui.cassonade.recipe.missing");

	private RecipeSheet() {
	}

	/** Draws the sheet beside the popup, on its right, or on its left where the screen ends. */
	public static void draw(GuiGraphicsExtractor graphics, Font font, RecipeVariant variant, int popupLeft, int popupRight, int top,
			int screenWidth, int screenHeight) {
		ItemStack result = variant.cassonade$result();
		Component name = result.getHoverName();
		Component status = variant.cassonade$craftable() ? CRAFTABLE : MISSING;
		int grid = variant.cassonade$smelting() ? CELL : GRID * CELL;
		int content = grid + GAP + ARROW + GAP + RESULT;
		int width = 2 * (BORDER + PADDING) + Math.max(content, Math.max(font.width(name), SmallText.width(font, status)));
		int height = 2 * (BORDER + PADDING) + font.lineHeight + 2 + SmallText.lineHeight(font) + PADDING + GRID * CELL;
		int left = popupRight + 2 + width <= screenWidth ? popupRight + 2 : popupLeft - 2 - width;
		top = Math.max(0, Math.min(top, screenHeight - height));

		// Above the recipe book's items and the window's
		graphics.nextStratum();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, left, top, width, height);
		int x = left + BORDER + PADDING;
		int y = top + BORDER + PADDING;
		graphics.text(font, name, x, y, INK, false);
		y += font.lineHeight + 2;
		small(graphics, font, status, x, y, variant.cassonade$craftable() ? GREEN_INK : RED_INK);
		y += SmallText.lineHeight(font) + PADDING;

		int shown = (int) (Util.getMillis() / CYCLE_MILLIS);
		if (variant.cassonade$smelting()) {
			int cellTop = y + CELL;
			cell(graphics, x, cellTop);
			for (RecipeVariant.Ingredient ingredient : variant.cassonade$ingredients()) {
				graphics.fakeItem(ingredient.shown(shown), x + 1, cellTop + 1);
			}
		} else {
			for (int row = 0; row < GRID; row++) {
				for (int column = 0; column < GRID; column++) {
					cell(graphics, x + column * CELL, y + row * CELL);
				}
			}
			for (RecipeVariant.Ingredient ingredient : variant.cassonade$ingredients()) {
				graphics.fakeItem(ingredient.shown(shown), x + ingredient.column() * CELL + 1, y + ingredient.row() * CELL + 1);
			}
		}

		int middle = y + GRID * CELL / 2;
		arrow(graphics, x + grid + GAP, middle);
		int resultLeft = x + grid + GAP + ARROW + GAP;
		int resultTop = middle - RESULT / 2;
		graphics.fill(resultLeft, resultTop, resultLeft + RESULT, resultTop + RESULT, CELL_FILL);
		graphics.fakeItem(result, resultLeft + 5, resultTop + 5);
		graphics.itemDecorations(font, result, resultLeft + 5, resultTop + 5);
	}

	/** A cell of the grid, a pixel apart from the next. */
	private static void cell(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x, y, x + CELL - 1, y + CELL - 1, CELL_FILL);
	}

	/** An inked arrow pointing right, centered on y. */
	private static void arrow(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x, y - 1, x + ARROW - 3, y + 1, INK);
		for (int i = 0; i < 4; i++) {
			graphics.fill(x + ARROW - 4 + i, y - 4 + i, x + ARROW - 3 + i, y + 4 - i, INK);
		}
	}

	private static void small(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
		float scale = SmallText.scale();
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, 0, 0, color, false);
		graphics.pose().popMatrix();
	}
}
