package com.cassonade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import com.cassonade.client.map.FramedPanel;

/**
 * Where the open chat sits: its history and, right below, its input field, two framed panels of the
 * same width stacked above the hearts and the food. In game, its messages stay where they are when it
 * is open.
 */
public final class ChatLayout {
	/** From the screen's left edge to the panels. */
	public static final int MARGIN = 2;
	/** Between the frame and the text. */
	public static final int PADDING = 3;
	/** Between the history and the input field. */
	private static final int GAP = 2;
	/** A line of text, its shadow included. */
	private static final int LINE_HEIGHT = 9;
	private static final int CAPITAL_HEIGHT = 7;
	public static final int INPUT_HEIGHT = 2 * (FramedPanel.BORDER + PADDING) + LINE_HEIGHT;
	/** The hearts and the row of armor above them, the food and the air bubbles on the right. */
	private static final int STATUS_BARS = 49;
	/** Between the rows of a popup and its edge: the rows leave 2 pixels above their text already. */
	private static final int POPUP_PADDING = 2;
	private static final int POPUP_SIDE_PADDING = 4;
	private static final int TOOLTIP_SPRITE_MARGIN = 8;
	private static final Identifier TOOLTIP_BACKGROUND = Identifier.withDefaultNamespace("tooltip/background");
	private static final Identifier TOOLTIP_FRAME = Identifier.withDefaultNamespace("tooltip/frame");
	/** Where the messages start. */
	public static final int TEXT_LEFT = MARGIN + FramedPanel.BORDER + PADDING;

	private ChatLayout() {
	}

	public static int inputTop(int guiHeight) {
		return guiHeight - STATUS_BARS - GAP - INPUT_HEIGHT - HudLayout.BOTTOM_MARGIN;
	}

	/** Where the input field's text is written: its capitals, 7 pixels high, in the middle of the frame. */
	public static int inputTextTop(int guiHeight) {
		return inputTop(guiHeight) + FramedPanel.BORDER + (INPUT_HEIGHT - 2 * FramedPanel.BORDER - CAPITAL_HEIGHT) / 2;
	}

	/**
	 * Where the rows of the commands' usage, errors and suggestions start, below the input field, over
	 * the gap left for the armor: inside a popup framed like the tooltips (drawPopup).
	 */
	public static int popupRowsTop(int guiHeight) {
		return inputTop(guiHeight) + INPUT_HEIGHT + GAP + POPUP_PADDING;
	}

	/** How many 12 pixel rows fit in the popup, above the screen's bottom. */
	public static int popupRows(int guiHeight) {
		return Math.max(1, (guiHeight - popupRowsTop(guiHeight) - POPUP_PADDING) / 12);
	}

	/**
	 * Draws a tooltip's background and frame around rows, their text 2 pixels below each row's top:
	 * the tooltips' sprites (TooltipRenderUtilMixin) start showing 8 pixels in.
	 */
	public static void drawPopup(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
		int left = x0 - POPUP_SIDE_PADDING - TOOLTIP_SPRITE_MARGIN;
		int top = y0 - POPUP_PADDING - TOOLTIP_SPRITE_MARGIN;
		int width = x1 - x0 + 2 * (POPUP_SIDE_PADDING + TOOLTIP_SPRITE_MARGIN);
		int height = y1 - y0 + 2 * (POPUP_PADDING + TOOLTIP_SPRITE_MARGIN);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TOOLTIP_BACKGROUND, left, top, width, height);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TOOLTIP_FRAME, left, top, width, height);
	}

	public static int historyBottom(int guiHeight) {
		return inputTop(guiHeight) - GAP;
	}

	/** Under the last message: above the line telling how many are queued, when some are. */
	public static int linesBottom(Minecraft minecraft, int guiHeight) {
		int bottom = historyBottom(guiHeight) - FramedPanel.BORDER - PADDING;
		if (minecraft.gui.chatListener().queueSize() > 0) {
			bottom -= Mth.ceil(LINE_HEIGHT * scale(minecraft));
		}
		return bottom;
	}

	/** The panels' right edge: the chat's width, its scroll bar and the padding. */
	public static int right(Minecraft minecraft) {
		float scale = scale(minecraft);
		int maxWidth = Mth.ceil(ChatComponent.getWidth(minecraft.options.chatWidth().get()) / scale);
		// Vanilla's scroll bar ends 6 pixels right of the messages' width
		return TEXT_LEFT + Mth.ceil((maxWidth + 6) * scale) + PADDING + FramedPanel.BORDER;
	}

	private static float scale(Minecraft minecraft) {
		return minecraft.options.chatScale().get().floatValue();
	}
}
