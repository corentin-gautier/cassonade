package com.vanillasugar.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.Mth;

import com.vanillasugar.client.map.FramedPanel;

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
	/** Where the messages start. */
	public static final int TEXT_LEFT = MARGIN + FramedPanel.BORDER + PADDING;

	private ChatLayout() {
	}

	public static int inputTop(int guiHeight) {
		return guiHeight - STATUS_BARS - GAP - INPUT_HEIGHT;
	}

	/** Where the input field's text is written: its capitals, 7 pixels high, in the middle of the frame. */
	public static int inputTextTop(int guiHeight) {
		return inputTop(guiHeight) + FramedPanel.BORDER + (INPUT_HEIGHT - 2 * FramedPanel.BORDER - CAPITAL_HEIGHT) / 2;
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
