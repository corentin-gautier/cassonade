package com.cassonade.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.layouts.Layout;

import com.cassonade.client.map.FramedPanel;

/** The panel behind vanilla's small dialogs (confirmations, alerts, server forms), so that their text never floats over the background. */
public final class DialogPanel {
	public static final int PADDING = 12;
	public static final int TITLE_COLOR = WorldSelectionScreen.GOLD;
	public static final int LABEL_COLOR = WorldSelectionScreen.MUTED;
	/** How wide a dialog's text may run: vanilla's screen width less a margin reads badly on a large window. */
	public static final int MAX_TEXT_WIDTH = 360;

	private DialogPanel() {
	}

	public static void around(GuiGraphicsExtractor graphics, Layout layout, int padding) {
		FramedPanel.draw(graphics, layout.getX() - padding, layout.getY() - padding,
				layout.getWidth() + 2 * padding, layout.getHeight() + 2 * padding);
	}
}
