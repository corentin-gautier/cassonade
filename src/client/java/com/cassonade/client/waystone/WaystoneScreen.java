package com.cassonade.client.waystone;

import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.cassonade.Cassonade;
import com.cassonade.client.map.FramedPanel;
import com.cassonade.compass.Coordinates;
import com.cassonade.waystone.WaystoneListPayload;
import com.cassonade.waystone.WaystoneRenamePayload;
import com.cassonade.waystone.WaystoneTravelPayload;
import com.cassonade.waystone.Waystones;

/**
 * This waystone's name on top, then the others: the named ones first, by name, a click goes there. A waystone without
 * a name goes by its dimension and coordinates. The name is kept on leaving, by a trip or by Esc, as in a book. The
 * wheel scrolls a long list.
 */
public class WaystoneScreen extends Screen {
	private static final int ROWS = 7;
	private static final int ROW_HEIGHT = 22;
	private static final int ROW_WIDTH = 260;
	private static final int NAME_HEIGHT = 20;
	private static final int PADDING = 8;
	private static final int BORDER = FramedPanel.BORDER;
	// The theme's colors (tools/gui_theme.py)
	private static final int GOLD = 0xFFF6DA8A;
	private static final int TEXT = 0xFFE8DABC;
	private static final int NETHER_TINT = 0x60B03020;
	private static final int END_TINT = 0x608A4FC0;

	private final WaystoneListPayload list;
	private final List<WaystoneListPayload.Destination> destinations;
	private String name;
	private int scroll;

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
		int rowWidth = panelWidth() - 2 * (BORDER + PADDING);
		int left = (width - rowWidth) / 2;
		EditBox nameBox = new EditBox(font, left, panelTop() + BORDER + PADDING + font.lineHeight + PADDING, rowWidth, NAME_HEIGHT,
				Component.translatable("gui.cassonade.waystone.name"));
		nameBox.setMaxLength(Waystones.MAX_NAME_LENGTH);
		nameBox.setValue(name);
		nameBox.setHint(defaultName(GlobalPos.of(minecraft.level.dimension(), list.from())));
		nameBox.setResponder(value -> name = value);
		addRenderableWidget(nameBox);
		for (int i = 0; i < shownRows(); i++) {
			WaystoneListPayload.Destination destination = destinations.get(i + scroll);
			Button button = new DestinationButton(left, listTop() + i * ROW_HEIGHT, rowWidth, ROW_HEIGHT - 2, destination, pressed -> go(destination));
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
		if (!name.strip().equals(list.name())) {
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
		int limit = destinations.size() - shownRows();
		int moved = Math.clamp(scroll - (int) Math.signum(scrollY), 0, limit);
		if (moved != scroll) {
			scroll = moved;
			rebuildWidgets();
		}
		return true;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		FramedPanel.draw(graphics, (width - panelWidth()) / 2, panelTop(), panelWidth(), panelHeight());
		graphics.centeredText(font, title, width / 2, panelTop() + BORDER + PADDING, GOLD);
		if (destinations.isEmpty()) {
			graphics.centeredText(font, Component.translatable("gui.cassonade.waystone.alone"), width / 2, listTop(), TEXT);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
