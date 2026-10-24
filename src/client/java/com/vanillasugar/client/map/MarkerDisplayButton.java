package com.vanillasugar.client.map;

import java.util.Locale;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import org.jspecify.annotations.Nullable;

import com.vanillasugar.client.ClientSettings;
import com.vanillasugar.client.ClientSettings.MarkerDisplay;
import com.vanillasugar.client.LabeledKeys;
import com.vanillasugar.client.VanillaSugarClient;

/**
 * Small button left of the minimap, under the rotation one, usable whenever the cursor is free
 * (inventory or chat open): cycles how markers are shown in the world, icon and text, icon only, or
 * not at all. The marker display key does the same in game, and is printed in the corner.
 */
public class MarkerDisplayButton extends Button {
	/** Room for the icon in the middle and the key in the corner. */
	public static final int SIZE = 24;
	/** The theme's WOOD_DARK, lighter than the panel's background. */
	static final int BACKGROUND_HOVERED = 0xFF473130;
	private static final int TEXT_LINES = 0xFFFFFFFF;
	private static final int CROSS = 0xFFE03030;
	/** The theme's LABEL, like the keys on the buttons in the bottom right corner. */
	private static final int KEY_COLOR = 0xFFE8DABC;

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

	/** Left out of the arrow and tab keys, which the chat keeps for its history and suggestions. */
	@Override
	public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
		return null;
	}

	/** Not focused once clicked either: enter still sends the message typed. */
	@Override
	public boolean shouldTakeFocusAfterInteraction() {
		return false;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x = getX();
		int y = getY();
		FramedPanel.draw(graphics, x, y, SIZE, SIZE);
		if (isHoveredOrFocused()) {
			graphics.fill(x + FramedPanel.BORDER, y + FramedPanel.BORDER, x + SIZE - FramedPanel.BORDER, y + SIZE - FramedPanel.BORDER, BACKGROUND_HOVERED);
		}

		int centerX = x + SIZE / 2;
		int centerY = y + SIZE / 2;
		MarkerDisplay display = ClientSettings.markerDisplay();
		if (display == MarkerDisplay.ICON_AND_TEXT) {
			// The banner with two lines of text beside it
			MapDecorations.banner(graphics, DyeColor.RED, centerX - 3, centerY);
			graphics.fill(centerX + 1, centerY - 3, centerX + 6, centerY - 2, TEXT_LINES);
			graphics.fill(centerX + 1, centerY + 1, centerX + 5, centerY + 2, TEXT_LINES);
		} else {
			MapDecorations.banner(graphics, DyeColor.RED, centerX, centerY);
		}
		if (display == MarkerDisplay.NONE) {
			for (int i = 0; i < 10; i++) {
				graphics.fill(centerX - 5 + i, centerY - 5 + i, centerX - 4 + i, centerY - 4 + i, CROSS);
				graphics.fill(centerX + 4 - i, centerY - 5 + i, centerX + 5 - i, centerY - 4 + i, CROSS);
			}
		}
		drawKey(graphics, VanillaSugarClient.MARKER_DISPLAY, x, y);
	}

	/** The key doing the same in game, in the bottom right corner like an item count; none when unbound. */
	static void drawKey(GuiGraphicsExtractor graphics, KeyMapping mapping, int x, int y) {
		if (mapping.isUnbound()) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		Component key = LabeledKeys.printedName(mapping);
		SmallText.left(graphics, font, key, x + SIZE - 2 - SmallText.width(font, key), y + SIZE - 2 - SmallText.lineHeight(font), KEY_COLOR);
	}
}
