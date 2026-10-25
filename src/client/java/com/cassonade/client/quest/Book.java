package com.cassonade.client.quest;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import com.cassonade.Cassonade;
import com.cassonade.client.map.SmallText;

/**
 * The open book the quest journal and the advancements are written in: a leather cover around two
 * cream pages, ink without shadows, titles in large letters, descriptions in small ones, and
 * clickable labels drawn as small ink-framed tags.
 */
final class Book {
	private static final int MARGIN = 6;
	private static final int MAX_WIDTH = 600;
	private static final int MAX_HEIGHT = 380;
	/** Keep in sync with COVER_BORDER in tools/quest_textures.py, less the pages' overlap. */
	static final int COVER = 10;
	private static final int SPINE = 6;
	static final int PADDING = 14;
	static final int ICON_SIZE = 16;
	/** An icon and the title beside it, centered on each other. */
	static final int ICON_ROW = 18;
	private static final int TITLE_SCALE = 2;
	private static final Identifier COVER_SPRITE = Cassonade.id("quest/journal_cover");
	private static final Component ELLIPSIS = Component.literal("…");

	// The theme's colors (tools/gui_theme.py), and inks dark enough to read on the paper
	static final int PAPER = 0xFFECE2C6;
	private static final int PAPER_SHADOW = 0xFFC9BA96;
	static final int PAGE_EDGE = 0xFFB8A680;
	static final int INK = 0xFF3A2A18;
	static final int FADED_INK = 0xFF76664C;
	/** What cannot be done yet, paler than the faded ink. */
	static final int LOCKED_INK = 0xFFAC9C80;
	/** Over the icons of what cannot be done yet: items have no transparency of their own. */
	private static final int ICON_VEIL = 0x99ECE2C6;
	static final int GREEN_INK = 0xFF3E6A1A;
	static final int GOLD_INK = 0xFF7A5410;
	static final int PURPLE_INK = 0xFF5A2A80;
	static final int RED_INK = 0xFF8E2418;
	static final int RIBBON = 0xFFA82A24;
	/** Behind what stands out: the chapter open, the quest followed. */
	static final int HIGHLIGHT = 0xFFDCCDA6;
	static final int HOVERED = 0xFFE4D8B8;
	/** How far the background of what stands out reaches past it, on both sides. */
	static final int SELECTION_PADDING = 5;
	private static final int RIBBON_WIDTH = 2;
	private static final int BAR_BACKGROUND = 0xFFC9BA96;
	private static final int BAR_FILL = 0xFF6E9234;

	private Book() {
	}

	/** Where the book and its pages are, centered on the screen. */
	record Layout(int left, int top, int width, int height) {
		static Layout centered(int screenWidth, int screenHeight) {
			int width = Math.min(screenWidth - 2 * MARGIN, MAX_WIDTH);
			int height = Math.min(screenHeight - 2 * MARGIN, MAX_HEIGHT);
			return new Layout((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
		}

		int spineX() {
			return left + width / 2;
		}

		int pagesTop() {
			return top + COVER;
		}

		int pagesBottom() {
			return top + height - COVER - 2;
		}

		int leftPageLeft() {
			return left + COVER + PADDING;
		}

		int leftPageRight() {
			return spineX() - SPINE - PADDING / 2;
		}

		int rightPageLeft() {
			return spineX() + SPINE + PADDING / 2;
		}

		int rightPageRight() {
			return left + width - COVER - PADDING;
		}
	}

	/** The clickable areas drawn this frame. */
	static final class Links {
		private record Link(ScreenRectangle area, Runnable action) {
		}

		private final List<Link> links = new ArrayList<>();

		void clear() {
			links.clear();
		}

		void add(ScreenRectangle area, Runnable action) {
			links.add(new Link(area, action));
		}

		int size() {
			return links.size();
		}

		/** Drops those added since the index that stick out of the band, scrolled out of a page. */
		void clip(int from, int top, int bottom) {
			links.subList(from, links.size()).removeIf(link -> link.area.top() < top || link.area.bottom() > bottom);
		}

		boolean click(double x, double y) {
			for (Link link : links) {
				if (link.area.containsPoint((int) x, (int) y)) {
					link.action.run();
					return true;
				}
			}
			return false;
		}
	}

	static void draw(GuiGraphicsExtractor graphics, Layout book) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, COVER_SPRITE, book.left, book.top, book.width, book.height);
		int left = book.left + COVER;
		int right = book.left + book.width - COVER;
		// The pages below show at the bottom edge
		graphics.fill(left + 1, book.pagesBottom(), right - 1, book.pagesBottom() + 2, PAGE_EDGE);
		graphics.fill(left, book.pagesTop(), right, book.pagesBottom(), PAPER);
		// The spine's shadow, darker towards the fold
		int spine = book.spineX();
		for (int i = 0; i < SPINE; i++) {
			int shade = i < 2 ? PAPER_SHADOW : blend(PAPER_SHADOW, PAPER, i / (float) SPINE);
			graphics.fill(spine - i - 1, book.pagesTop(), spine - i, book.pagesBottom(), shade);
			graphics.fill(spine + i, book.pagesTop(), spine + i + 1, book.pagesBottom(), shade);
		}
		graphics.fill(spine - 1, book.pagesTop(), spine + 1, book.pagesBottom(), PAGE_EDGE);
	}

	private static int blend(int from, int to, float t) {
		int r = Mth.lerpInt(t, from >> 16 & 0xFF, to >> 16 & 0xFF);
		int g = Mth.lerpInt(t, from >> 8 & 0xFF, to >> 8 & 0xFF);
		int b = Mth.lerpInt(t, from & 0xFF, to & 0xFF);
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	/**
	 * The background of a row standing out on either page, past its sides by SELECTION_PADDING: the
	 * chosen one (chapter open, quest followed, advancement unfolded) with a ribbon along its left
	 * edge, the hovered one lighter.
	 */
	static void selection(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, boolean chosen) {
		graphics.fill(left - SELECTION_PADDING, top, right + SELECTION_PADDING, bottom, chosen ? HIGHLIGHT : HOVERED);
		if (chosen) {
			graphics.fill(left - SELECTION_PADDING, top, left - SELECTION_PADDING + RIBBON_WIDTH, bottom, RIBBON);
		}
	}

	/** A heading in large letters, smaller when it would not fit the width. Returns the y below it. */
	static int heading(GuiGraphicsExtractor graphics, Font font, Component text, int left, int right, int y) {
		int scale = headingScale(font, text, right - left);
		graphics.pose().pushMatrix();
		graphics.pose().translate((left + right) / 2f, y);
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, -font.width(text) / 2, 0, INK, false);
		graphics.pose().popMatrix();
		return y + font.lineHeight * scale;
	}

	private static int headingScale(Font font, Component text, int width) {
		return font.width(text) * TITLE_SCALE <= width ? TITLE_SCALE : 1;
	}

	static int headingHeight(Font font, Component text, int width) {
		return font.lineHeight * headingScale(font, text, width);
	}

	/** Veils the icons drawn in the areas with the paper's color, above them. */
	static void dim(GuiGraphicsExtractor graphics, List<ScreenRectangle> icons) {
		if (icons.isEmpty()) {
			return;
		}
		// Items are drawn after the fills of their stratum: the veil goes in the next one
		graphics.nextStratum();
		for (ScreenRectangle icon : icons) {
			graphics.fill(icon.left(), icon.top(), icon.right(), icon.bottom(), ICON_VEIL);
		}
	}

	/** A line across the page, a small ribbon diamond in its middle. */
	static void rule(GuiGraphicsExtractor graphics, int left, int right, int y) {
		int center = (left + right) / 2;
		graphics.fill(left, y, right, y + 1, PAGE_EDGE);
		for (int i = 0; i < 3; i++) {
			graphics.fill(center - i, y - 2 + i, center + i + 1, y - 1 + i, RIBBON);
			graphics.fill(center - i, y + 3 - i, center + i + 1, y + 4 - i, RIBBON);
		}
	}

	/** An icon with its title beside it, both centered on the row. */
	static void iconRow(GuiGraphicsExtractor graphics, Font font, ItemStack icon, FormattedCharSequence title, int x, int top, int color) {
		graphics.fakeItem(icon, x, top + (ICON_ROW - ICON_SIZE) / 2);
		graphics.text(font, title, x + ICON_SIZE + 6, top + (ICON_ROW - font.lineHeight) / 2 + 1, color, false);
	}

	// --- Small text, one GUI scale step smaller (see SmallText), without shadow on the paper ------

	static int smallLine(Font font) {
		return SmallText.lineHeight(font) + 1;
	}

	static int smallWidth(Font font, Component text) {
		return SmallText.width(font, text);
	}

	static List<FormattedCharSequence> splitSmall(Font font, Component text, int width) {
		return font.split(text, (int) (width / SmallText.scale()));
	}

	static void small(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence line, float x, float y, int color) {
		float scale = SmallText.scale();
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.text(font, line, 0, 0, color, false);
		graphics.pose().popMatrix();
	}

	static void small(GuiGraphicsExtractor graphics, Font font, Component text, float x, float y, int color) {
		small(graphics, font, text.getVisualOrderText(), x, y, color);
	}

	/** Small lines from the top, returns the y below them. */
	static int smallLines(GuiGraphicsExtractor graphics, Font font, List<FormattedCharSequence> lines, int x, int y, int color) {
		for (FormattedCharSequence line : lines) {
			small(graphics, font, line, x, y, color);
			y += smallLine(font);
		}
		return y;
	}

	// --- Tags, bars and scrollbars ------------------------------------------------------------------

	static int tagHeight(Font font) {
		return SmallText.lineHeight(font) + 4;
	}

	static int tagWidth(Font font, Component text) {
		return smallWidth(font, text) + 8;
	}

	/**
	 * A small label framed in red ink, its top left at x, y: filled when it is the current choice or
	 * hovered, clickable when it has an action. Returns its width.
	 */
	static int tag(GuiGraphicsExtractor graphics, Font font, Links links, Component text, int x, int y, boolean selected,
			int mouseX, int mouseY, @Nullable Runnable action) {
		int width = tagWidth(font, text);
		int height = tagHeight(font);
		boolean hovered = action != null && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		if (selected || hovered) {
			graphics.fill(x, y, x + width, y + height, RED_INK);
		} else {
			graphics.outline(x, y, width, height, RED_INK);
		}
		small(graphics, font, text, x + 4, y + 2.5F, selected || hovered ? PAPER : RED_INK);
		if (action != null) {
			links.add(new ScreenRectangle(x, y, width, height), action);
		}
		return width;
	}

	static void bar(GuiGraphicsExtractor graphics, int left, int y, int right, int met, int total) {
		graphics.fill(left, y, right, y + 3, BAR_BACKGROUND);
		graphics.fill(left, y, left + (right - left) * met / Math.max(1, total), y + 3, BAR_FILL);
	}

	static void scrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom, double scroll, int contentHeight) {
		int trackHeight = bottom - top;
		if (contentHeight <= trackHeight) {
			return;
		}
		int thumbHeight = Math.max(12, trackHeight * trackHeight / contentHeight);
		int thumbTop = top + (int) ((trackHeight - thumbHeight) * scroll / (contentHeight - trackHeight));
		graphics.fill(x, thumbTop, x + 2, thumbTop + thumbHeight, PAGE_EDGE);
	}

	/** The text cut to fit the width, with an ellipsis when it does not. */
	static FormattedCharSequence ellipsize(Font font, Component text, int width) {
		if (font.width(text) <= width) {
			return text.getVisualOrderText();
		}
		FormattedText cut = font.substrByWidth(text, width - font.width(ELLIPSIS));
		return Language.getInstance().getVisualOrder(FormattedText.composite(cut, ELLIPSIS));
	}
}
