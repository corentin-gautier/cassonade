package com.vanillasugar.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import com.vanillasugar.client.ClientSettings;
import com.vanillasugar.client.VanillaSugarClient;

/**
 * Small button left of the minimap, above the marker display one: keeps north up on the minimap, or
 * turns it with the player. Shows N for north up, the player's arrow when it turns. The minimap rotation
 * key does the same in game, and is printed in the corner.
 */
public class MinimapRotationButton extends Button {
	private static final int SIZE = MarkerDisplayButton.SIZE;
	private static final int BACKGROUND_HOVERED = MarkerDisplayButton.BACKGROUND_HOVERED;
	private static final int NORTH_COLOR = 0xFFE03030;

	public MinimapRotationButton(int guiWidth) {
		super(Minimap.frameLeft(guiWidth) - 2 - SIZE, Minimap.FRAME_TOP, SIZE, SIZE, Component.empty(), button -> {
			ClientSettings.setMinimapRotates(!ClientSettings.minimapRotates());
			((MinimapRotationButton) button).updateTooltip();
		}, DEFAULT_NARRATION);
		updateTooltip();
	}

	/** Switches between north up and turning, telling the player in the action bar. */
	public static void toggle(Minecraft minecraft) {
		ClientSettings.setMinimapRotates(!ClientSettings.minimapRotates());
		minecraft.gui.hud.setOverlayMessage(description(), false);
	}

	private static Component description() {
		return Component.translatable(ClientSettings.minimapRotates() ? "gui.vanillasugar.minimap.rotating" : "gui.vanillasugar.minimap.north_up");
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
		if (ClientSettings.minimapRotates()) {
			MapDecorations.player(graphics, x + SIZE / 2f, y + SIZE / 2f, 180);
		} else {
			graphics.centeredText(Minecraft.getInstance().font, "N", x + SIZE / 2, y + (SIZE - 8) / 2, NORTH_COLOR);
		}
		MarkerDisplayButton.drawKey(graphics, VanillaSugarClient.MINIMAP_ROTATION, x, y);
	}
}
