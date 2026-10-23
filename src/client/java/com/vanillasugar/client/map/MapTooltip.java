package com.vanillasugar.client.map;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import com.vanillasugar.storage.MapPreview;

/**
 * A filled map's tooltip: the map at half its size in the minimap's frame, then the game's lines about it
 * in small text. The server only sends a map's contents while it is in the player's inventory, so a map seen
 * in a chest before that keeps the game's tooltip.
 */
public class MapTooltip implements ClientTooltipComponent {
	private static final int MAP_SIZE = 64;
	private static final int SIZE = MAP_SIZE + 2 * FramedPanel.BORDER;
	private static final int MARGIN = 2;
	/** Between the frame and the lines under it. */
	private static final int TEXT_GAP = 4;
	private static final int TEXT_COLOR = 0xFFAAAAAA;

	private final MapPreview preview;
	private final MapItemSavedData data;
	private final List<Component> lines = new ArrayList<>();
	private final MapRenderState renderState = new MapRenderState();

	public MapTooltip(MapPreview preview) {
		Minecraft minecraft = Minecraft.getInstance();
		this.preview = preview;
		this.data = minecraft.level == null ? null : MapItem.getSavedData(preview.id(), minecraft.level);
		if (data != null) {
			TooltipFlag flag = minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
			preview.id().addToTooltip(Item.TooltipContext.of(minecraft.level), lines::add, flag, preview.stack());
		}
	}

	private int textHeight(Font font) {
		return lines.isEmpty() ? 0 : TEXT_GAP + lines.size() * (SmallText.lineHeight(font) + 1);
	}

	@Override
	public int getHeight(Font font) {
		return data == null ? 0 : MARGIN + SIZE + textHeight(font) + MARGIN;
	}

	@Override
	public int getWidth(Font font) {
		if (data == null) {
			return 0;
		}
		int width = SIZE;
		for (Component line : lines) {
			width = Math.max(width, SmallText.width(font, line));
		}
		return width;
	}

	@Override
	public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
		if (data == null) {
			return;
		}
		int top = y + MARGIN;
		FramedPanel.draw(graphics, x, top, SIZE, SIZE);
		graphics.pose().pushMatrix();
		graphics.pose().translate(x + FramedPanel.BORDER, top + FramedPanel.BORDER);
		graphics.pose().scale(MAP_SIZE / 128f, MAP_SIZE / 128f);
		Minecraft.getInstance().getMapRenderer().extractRenderState(preview.id(), data, renderState);
		graphics.map(renderState);
		graphics.pose().popMatrix();

		int lineY = top + SIZE + TEXT_GAP;
		for (Component line : lines) {
			SmallText.left(graphics, font, line, x, lineY, TEXT_COLOR);
			lineY += SmallText.lineHeight(font) + 1;
		}
	}
}
