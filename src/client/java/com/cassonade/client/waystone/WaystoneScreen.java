package com.cassonade.client.waystone;

import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.cassonade.Cassonade;
import com.cassonade.client.WorldSelectionScreen;
import com.cassonade.client.map.FramedPanel;
import com.cassonade.compass.Coordinates;
import com.cassonade.waystone.WaystoneListPayload;
import com.cassonade.waystone.WaystoneRenamePayload;
import com.cassonade.waystone.WaystoneTravelPayload;
import com.cassonade.waystone.Waystones;

/**
 * This waystone's name on top, then the others: the named ones first, by name, a click goes there. A waystone without
 * a name goes by its dimension and coordinates. Only its owner can change the name, which is kept on leaving, by a trip or by Esc, as in a book. The
 * wheel scrolls a long list, or its scrollbar dragged, the same as the world list's.
 */
public class WaystoneScreen extends Screen {
	private static final int ROWS = 7;
	private static final int ROW_HEIGHT = 22;
	private static final int ROW_WIDTH = 260;
	private static final int NAME_HEIGHT = 20;
	private static final int PADDING = 8;
	private static final int BORDER = FramedPanel.BORDER;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int MIN_HANDLE_HEIGHT = 32;
	// The theme's colors (tools/gui_theme.py)
	private static final int GOLD = 0xFFF6DA8A;
	private static final int TEXT = 0xFFE8DABC;
	private static final int NETHER_TINT = 0x60B03020;
	private static final int END_TINT = 0x608A4FC0;

	private final WaystoneListPayload list;
	private final List<WaystoneListPayload.Destination> destinations;
	private String name;
	private int scroll;
	private boolean dragging;

	private WaystoneScreen(WaystoneListPayload list) {
		super(Component.translatable("block.cassonade.waystone"));
		this.list = list;
		name = list.name();
		destinations = list.destinations().stream()
				.sorted(Comparator.comparing((WaystoneListPayload.Destination destination) -> destination.name().isEmpty())
						.thenComparing(WaystoneListPayload.Destination::name)
						.thenComparing(destination -> destination.pos().dimension().identifier().toString())
						.thenComparing(destination -> destination.pos().pos()))
				.toList();
	}

	public static void open(Minecraft minecraft, WaystoneListPayload list) {
		minecraft.gui.setScreen(new WaystoneScreen(list));
	}

	private int shownRows() {
		return Math.min(ROWS, destinations.size());
	}

	private int maxScroll() {
		return destinations.size() - shownRows();
	}

	private int rowWidth() {
		return panelWidth() - 2 * (BORDER + PADDING);
	}

	private int rowLeft() {
		return (width - rowWidth()) / 2;
	}

	private int scrollbarX() {
		return rowLeft() + rowWidth() - SCROLLBAR_WIDTH;
	}

	/** The groove runs along the rows, from the first one's top to the last one's bottom, the gap below it left out. */
	private int grooveTop() {
		return listTop();
	}

	private int grooveBottom() {
		return listTop() + listHeight() - 2;
	}

	private int handleHeight() {
		int groove = grooveBottom() - grooveTop();
		return Math.min(groove, Math.max(MIN_HANDLE_HEIGHT, groove * shownRows() / destinations.size()));
	}

	private boolean isOverScrollbar(double x, double y) {
		return maxScroll() > 0 && x >= scrollbarX() && x < scrollbarX() + SCROLLBAR_WIDTH && y >= grooveTop() && y < grooveBottom();
	}

	private int panelWidth() {
		return Math.min(width - 32, ROW_WIDTH + 2 * (BORDER + PADDING));
	}

	/** With no other waystone, one line saying so where the list would be. */
	private int listHeight() {
		return destinations.isEmpty() ? font.lineHeight : shownRows() * ROW_HEIGHT;
	}

	private int panelHeight() {
		return 2 * (BORDER + PADDING) + font.lineHeight + PADDING + NAME_HEIGHT + PADDING + listHeight();
	}

	private int panelTop() {
		return (height - panelHeight()) / 2;
	}

	private int listTop() {
		return panelTop() + BORDER + PADDING + font.lineHeight + PADDING + NAME_HEIGHT + PADDING;
	}

	@Override
	protected void init() {
		int rowWidth = rowWidth();
		int left = rowLeft();
		EditBox nameBox = new EditBox(font, left, panelTop() + BORDER + PADDING + font.lineHeight + PADDING, rowWidth, NAME_HEIGHT,
				Component.translatable("gui.cassonade.waystone.name"));
		nameBox.setMaxLength(Waystones.MAX_NAME_LENGTH);
		nameBox.setValue(name);
		nameBox.setHint(defaultName(GlobalPos.of(minecraft.level.dimension(), list.from())));
		nameBox.setResponder(value -> name = value);
		nameBox.setEditable(list.canRename());
		addRenderableWidget(nameBox);
		for (int i = 0; i < shownRows(); i++) {
			WaystoneListPayload.Destination destination = destinations.get(i + scroll);
			// Short of the scrollbar when the list needs one
			int buttonWidth = rowWidth - (maxScroll() > 0 ? WorldSelectionScreen.SCROLLBAR_SPACE : 0);
			Button button = new DestinationButton(left, listTop() + i * ROW_HEIGHT, buttonWidth, ROW_HEIGHT - 2, destination, pressed -> go(destination));
			if (!destination.name().isEmpty()) {
				button.setTooltip(Tooltip.create(defaultName(destination.pos())));
			}
			addRenderableWidget(button);
		}
	}

	/** A row of the list, with its dimension's icon and, away from the overworld, its color: told apart by both, and the label or the tooltip names it. */
	private static final class DestinationButton extends Button.Plain {
		private static final int ICON_SIZE = 10;
		private static final int ICON_INSET = 5;
		private final Identifier icon;
		private final int tint;

		private DestinationButton(int x, int y, int width, int height, WaystoneListPayload.Destination destination, OnPress onPress) {
			super(x, y, width, height, label(destination), onPress, DEFAULT_NARRATION);
			ResourceKey<Level> dimension = destination.pos().dimension();
			boolean known = dimension == Level.OVERWORLD || dimension == Level.NETHER || dimension == Level.END;
			icon = Cassonade.id("waystone/" + (known ? dimension.identifier().getPath() : "other"));
			tint = dimension == Level.NETHER ? NETHER_TINT : dimension == Level.END ? END_TINT : 0;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			extractDefaultSprite(graphics);
			if (tint != 0) {
				graphics.fill(getX(), getY(), getRight(), getBottom(), tint);
			}
			extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, getX() + ICON_INSET, getY() + (getHeight() - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE);
		}
	}

	private static Component label(WaystoneListPayload.Destination destination) {
		return destination.name().isEmpty() ? defaultName(destination.pos()) : Component.literal(destination.name());
	}

	private static Component defaultName(GlobalPos pos) {
		Component dimension = Component.translatable("gui.cassonade.waystone.dimension." + pos.dimension().identifier().getPath());
		return Component.translatable("gui.cassonade.waystone.default_name", dimension, Component.literal(Coordinates.of(pos.pos())));
	}

	private void saveName() {
		if (list.canRename() && !name.strip().equals(list.name())) {
			ClientPlayNetworking.send(new WaystoneRenamePayload(list.from(), name));
		}
	}

	private void go(WaystoneListPayload.Destination destination) {
		saveName();
		ClientPlayNetworking.send(new WaystoneTravelPayload(list.from(), destination.pos()));
		super.onClose();
	}

	@Override
	public void onClose() {
		saveName();
		super.onClose();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_RETURN) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scrollTo(scroll - (int) Math.signum(scrollY));
		return true;
	}

	private void scrollTo(int row) {
		int moved = Math.clamp(row, 0, maxScroll());
		if (moved != scroll) {
			scroll = moved;
			rebuildWidgets();
		}
	}

	/** The handle's middle follows the mouse, the list moving by whole rows. */
	private void dragTo(double y) {
		int track = grooveBottom() - grooveTop() - handleHeight();
		if (track > 0) {
			scrollTo((int) Math.round((y - grooveTop() - handleHeight() / 2.0) / track * maxScroll()));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && isOverScrollbar(event.x(), event.y())) {
			dragging = true;
			dragTo(event.y());
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			dragTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging) {
			dragging = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		FramedPanel.draw(graphics, (width - panelWidth()) / 2, panelTop(), panelWidth(), panelHeight());
		graphics.centeredText(font, title, width / 2, panelTop() + BORDER + PADDING, GOLD);
		if (destinations.isEmpty()) {
			graphics.centeredText(font, Component.translatable("gui.cassonade.waystone.alone"), width / 2, listTop(), TEXT);
		}
		if (maxScroll() > 0) {
			boolean hovered = dragging || isOverScrollbar(mouseX, mouseY);
			// The helper starts its groove 2 pixels into the list, where its first card begins
			WorldSelectionScreen.extractScrollbar(graphics, scrollbarX(), grooveTop() - 2, grooveBottom(), SCROLLBAR_WIDTH,
					(double) scroll / maxScroll(), handleHeight(), hovered);
			if (hovered) {
				graphics.requestCursor(dragging ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
