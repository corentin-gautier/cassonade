package com.cassonade.client.map;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import com.cassonade.Cassonade;

/** The minimap's frame around an opaque background: the clock, the biome name, the world map's labels. */
public final class FramedPanel {
	public static final int BORDER = 4;
	// Nine-slice frame, see tools/gui_style.py
	public static final Identifier FRAME = Cassonade.id("hud/minimap_frame");
	/** The HUD background (tools/gui_theme.py). */
	public static final int BACKGROUND_COLOR = 0xFF2B1F1F;

	private FramedPanel() {
	}

	/** Draws the panel, its frame included in the size. */
	public static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		graphics.fill(x + BORDER, y + BORDER, x + width - BORDER, y + height - BORDER, BACKGROUND_COLOR);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, x, y, width, height);
	}
}
