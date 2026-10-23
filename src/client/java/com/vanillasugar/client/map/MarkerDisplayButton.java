package com.vanillasugar.client.map;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import com.vanillasugar.client.ClientSettings;
import com.vanillasugar.client.ClientSettings.MarkerDisplay;

/**
 * Small button left of the minimap, under the rotation one, usable whenever the cursor is free
 * (inventory or chat open): cycles how markers are shown in the world, icon and text, icon only, or
 * not at all. The marker display key does the same in game.
 */
public class MarkerDisplayButton extends Button {
	public static final int SIZE = 20;
	/** The theme's WOOD_DARK, lighter than the panel's background. */
	static final int BACKGROUND_HOVERED = 0xFF473130;
	private static final int TEXT_LINES = 0xFFFFFFFF;
	private static final int CROSS = 0xFFE03030;

	public MarkerDisplayButton(int guiWidth) {
		super(Minimap.frameLeft(guiWidth) - 2 - SIZE, Minimap.FRAME_TOP + SIZE + 2, SIZE, SIZE, Component.empty(), button -> {
			ClientSettings.setMarkerDisplay(ClientSettings.markerDisplay().next());
			((MarkerDisplayButton) button).updateTooltip();
		}, DEFAULT_NARRATION);
		updateTooltip();
	}

	/** Moves to the next display, telling the player in the action bar. */
	public static void cycle(Minecraft minecraft) {
		ClientSettings.setMarkerDisplay(ClientSettings.markerDisplay().next());
		minecraft.gui.hud.setOverlayMessage(description(), false);
	}

	private static Component description() {
		return Component.translatable("gui.vanillasugar.marker_display",
				Component.translatable("gui.vanillasugar.marker_display." + ClientSettings.markerDisplay().name().toLowerCase(Locale.ROOT)));
	}

	private void updateTooltip() {
		setMessage(description());
		setTooltip(Tooltip.create(description()));
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x = getX();
		int y = getY();
		FramedPanel.draw(graphics, x, y, SIZE, SIZE);
		if (isHoveredOrFocused()) {
			graphics.fill(x + FramedPanel.BORDER, y + FramedPanel.BORDER, x + SIZE - FramedPanel.BORDER, y + SIZE - FramedPanel.BORDER, BACKGROUND_HOVERED);
		}

		MarkerDisplay display = ClientSettings.markerDisplay();
		if (display == MarkerDisplay.ICON_AND_TEXT) {
			// The banner with two lines of text beside it
			MapDecorations.banner(graphics, DyeColor.RED, x + 7, y + SIZE / 2f);
			graphics.fill(x + 11, y + 7, x + 16, y + 8, TEXT_LINES);
			graphics.fill(x + 11, y + 11, x + 15, y + 12, TEXT_LINES);
		} else {
			MapDecorations.banner(graphics, DyeColor.RED, x + SIZE / 2f, y + SIZE / 2f);
		}
		if (display == MarkerDisplay.NONE) {
			for (int i = 0; i < 10; i++) {
				graphics.fill(x + 5 + i, y + 5 + i, x + 6 + i, y + 6 + i, CROSS);
				graphics.fill(x + 14 - i, y + 5 + i, x + 15 - i, y + 6 + i, CROSS);
			}
		}
	}
}
