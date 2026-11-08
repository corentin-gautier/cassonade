package com.cassonade.client.waystone;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLMouse;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;

import com.cassonade.Cassonade;
import com.cassonade.client.ContainerPanel;
import com.cassonade.client.ScrollSounds;
import com.cassonade.client.WorldSelectionScreen;
import com.cassonade.compass.Coordinates;
import com.cassonade.waystone.WaystoneData;
import com.cassonade.waystone.WaystoneListPayload;
import com.cassonade.waystone.WaystoneMovePayload;
import com.cassonade.waystone.WaystoneRenamePayload;
import com.cassonade.waystone.WaystoneTravelPayload;
import com.cassonade.waystone.Waystones;

/**
 * This waystone's name on top, then the others, in the order the player gave them (the named ones first, by name, until
 * then), a click goes there. The faint arrows on the left of each row move it up or down, to the top or the
 * bottom with Shift, as in the server list. A waystone without
 * a name goes by its dimension and coordinates. Only its owner can change the name, which is kept on leaving, by a trip or by Esc, as in a book. The
 * wheel scrolls a long list, or its scrollbar dragged, the same as the world list's, ticking at each row. The window is
 * wood, each waystone a dark slot, as the hitching post's.
 */
public class WaystoneScreen extends Screen {
	private static final int ROWS = 7;
	private static final int ROW_HEIGHT = 22;
	private static final int ROW_WIDTH = 260;
	private static final int NAME_HEIGHT = 20;
	private static final int PADDING = 6;
	/** The window's rounded edge. */
	private static final int BORDER = 4;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int MIN_HANDLE_HEIGHT = 32;
	// The server list's arrows, cut out of its 32 pixel sprites, in a column on the left of the rows
	private static final Identifier MOVE_UP = Identifier.withDefaultNamespace("server_list/move_up");
	private static final Identifier MOVE_DOWN = Identifier.withDefaultNamespace("server_list/move_down");
	private static final int SPRITE_SIZE = 32;
	private static final int ARROW_U = 3;
	private static final int ARROW_UP_V = 5;
	private static final int ARROW_DOWN_V = 20;
	private static final int ARROW_WIDTH = 11;
	private static final int ARROW_HEIGHT = 7;
	private static final int ARROWS_WIDTH = ARROW_WIDTH + 3;
	private static final float ARROW_ALPHA = 0.25F;
	private static final float ARROW_ROW_ALPHA = 0.45F;
	private static final float ARROW_UNDER_MOUSE_ALPHA = 0.8F;
	// The theme's colors (tools/gui_theme.py)
	private static final int TEXT = 0xFFE8DABC;
	private static final int HOVER = 0xFFE8DABC;
	private static final int NETHER_TINT = 0x60B03020;
	private static final int END_TINT = 0x608A4FC0;

	private final WaystoneListPayload list;
	// The player's own waystones, then the others', each in the player's order
	private final List<WaystoneListPayload.Destination> own = new ArrayList<>();
	private final List<WaystoneListPayload.Destination> others = new ArrayList<>();
	/** The rows as shown: the player's, then, when there are both, a heading (null) before the others'. */
	private final List<WaystoneListPayload.@Nullable Destination> rows = new ArrayList<>();
	private String name;
	private int scroll;
	private boolean dragging;

	private WaystoneScreen(WaystoneListPayload list) {
		super(Component.translatable("block.cassonade.waystone"));
		this.list = list;
		name = list.name();
		// The server sends them in the player's order
		for (WaystoneListPayload.Destination destination : list.destinations()) {
			(destination.own() ? own : others).add(destination);
		}
		layOutRows();
	}

	public static void open(Minecraft minecraft, WaystoneListPayload list) {
		minecraft.gui.setScreen(new WaystoneScreen(list));
	}

	private void layOutRows() {
		rows.clear();
		rows.addAll(own);
		if (!own.isEmpty() && !others.isEmpty()) {
			rows.add(null);
		}
		rows.addAll(others);
	}

	private List<WaystoneListPayload.Destination> group(WaystoneListPayload.Destination destination) {
		return destination.own() ? own : others;
	}

	private int shownRows() {
		return Math.min(ROWS, rows.size());
	}

	private int maxScroll() {
		return rows.size() - shownRows();
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
		return Math.min(groove, Math.max(MIN_HANDLE_HEIGHT, groove * shownRows() / rows.size()));
	}

	private boolean isOverScrollbar(double x, double y) {
		return maxScroll() > 0 && x >= scrollbarX() && x < scrollbarX() + SCROLLBAR_WIDTH && y >= grooveTop() && y < grooveBottom();
	}

	private int panelWidth() {
		return Math.min(width - 32, ROW_WIDTH + 2 * (BORDER + PADDING));
	}

	/** With no other waystone, one line saying so where the list would be. */
	private int listHeight() {
		return rows.isEmpty() ? font.lineHeight : shownRows() * ROW_HEIGHT;
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
			WaystoneListPayload.Destination destination = rows.get(i + scroll);
			if (destination == null) {
				continue;
			}
			Button button = new DestinationButton(buttonsLeft(), listTop() + i * ROW_HEIGHT, buttonsRight() - buttonsLeft(), ROW_HEIGHT - 2, destination,
					pressed -> go(destination));
			if (!destination.name().isEmpty()) {
				button.setTooltip(Tooltip.create(defaultName(destination.pos())));
			}
			addRenderableWidget(button);
		}
	}

	/** Only with something to sort: the arrows' column on the left of the rows. */
	private int buttonsLeft() {
		return rowLeft() + (own.size() > 1 || others.size() > 1 ? ARROWS_WIDTH : 0);
	}

	/** Short of the scrollbar when the list needs one. */
	private int buttonsRight() {
		return rowLeft() + rowWidth() - (maxScroll() > 0 ? WorldSelectionScreen.SCROLLBAR_SPACE : 0);
	}

	/** The index in the rows of the waystone under the mouse, its arrows included, or -1. */
	private int rowAt(double x, double y) {
		if (buttonsLeft() == rowLeft() || x < rowLeft() || x >= buttonsRight() || y < listTop()) {
			return -1;
		}
		int shown = (int) ((y - listTop()) / ROW_HEIGHT);
		boolean inGap = y - listTop() - shown * ROW_HEIGHT >= ROW_HEIGHT - 2;
		return shown < shownRows() && !inGap && rows.get(shown + scroll) != null ? shown + scroll : -1;
	}

	/** Over an arrow of its row: up in the upper half of the column, down in the lower, where the row can still go that way. */
	private boolean overArrow(double x, double y, boolean up) {
		int row = rowAt(x, y);
		if (row < 0 || x >= rowLeft() + ARROW_WIDTH + 1) {
			return false;
		}
		double middle = listTop() + (row - scroll) * ROW_HEIGHT + (ROW_HEIGHT - 2) / 2.0;
		return up ? canMove(row, true) && y < middle : canMove(row, false) && y >= middle;
	}

	/** Within its group: the player's own and the others' are sorted apart. */
	private boolean canMove(int row, boolean up) {
		WaystoneListPayload.Destination destination = rows.get(row);
		List<WaystoneListPayload.Destination> group = group(destination);
		int index = group.indexOf(destination);
		return up ? index > 0 : index < group.size() - 1;
	}

	/**
	 * Moves a row of the list a step up or down, or to an end, here and on the server. The mouse goes with it, so the
	 * same arrow can be clicked again and again.
	 */
	private void move(int row, boolean up, boolean toEnd) {
		WaystoneListPayload.Destination destination = rows.get(row);
		if (destination == null || !WaystoneData.moveIn(group(destination), destination, up, toEnd)) {
			return;
		}
		layOutRows();
		ClientPlayNetworking.send(new WaystoneMovePayload(list.from(), destination.pos(), up, toEnd));
		int shownBefore = row - scroll;
		int moved = rows.indexOf(destination);
		// The moved row stays in view
		scroll = Math.clamp(scroll, Math.max(0, moved - shownRows() + 1), Math.min(moved, maxScroll()));
		rebuildWidgets();
		followMouse((moved - scroll - shownBefore) * ROW_HEIGHT);
	}

	/** Moves the pointer down by so many GUI pixels, as the game moves it when it frees the mouse. */
	private void followMouse(int dy) {
		if (dy == 0) {
			return;
		}
		Window window = minecraft.getWindow();
		double scale = (double) window.getScreenHeight() / window.getGuiScaledHeight();
		SDLMouse.SDL_WarpMouseInWindow(window.handle(), (float) minecraft.mouseHandler.xpos(), (float) (minecraft.mouseHandler.ypos() + dy * scale));
	}

	/**
	 * The server list's arrows on the left of every row that can move, faint so the list stays calm: those of the row
	 * under the mouse a little brighter, the one under the mouse brighter still.
	 */
	private void extractArrows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (buttonsLeft() == rowLeft()) {
			return;
		}
		int hovered = rowAt(mouseX, mouseY);
		for (int i = 0; i < shownRows(); i++) {
			int row = i + scroll;
			if (rows.get(row) == null) {
				continue;
			}
			int middle = listTop() + i * ROW_HEIGHT + (ROW_HEIGHT - 2) / 2;
			for (boolean up : new boolean[] {true, false}) {
				if (canMove(row, up)) {
					float alpha = row != hovered ? ARROW_ALPHA : overArrow(mouseX, mouseY, up) ? ARROW_UNDER_MOUSE_ALPHA : ARROW_ROW_ALPHA;
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, up ? MOVE_UP : MOVE_DOWN, SPRITE_SIZE, SPRITE_SIZE, ARROW_U,
							up ? ARROW_UP_V : ARROW_DOWN_V, rowLeft(), up ? middle - 1 - ARROW_HEIGHT : middle + 1, ARROW_WIDTH, ARROW_HEIGHT,
							ARGB.white(alpha));
				}
			}
		}
	}

	/** Over the others' waystones, in the row left for it, when the player has some of their own. */
	private void extractHeading(GuiGraphicsExtractor graphics) {
		int heading = rows.indexOf(null) - scroll;
		if (heading >= 0 && heading < shownRows()) {
			int top = listTop() + heading * ROW_HEIGHT;
			graphics.text(font, Component.translatable("gui.cassonade.waystone.others"), buttonsLeft(), top + (ROW_HEIGHT - 2 - font.lineHeight) / 2 + 1,
					ContainerPanel.LABEL, false);
		}
	}

	/**
	 * A row of the list, a dark slot framed in cream under the mouse, with its dimension's icon and, away from the
	 * overworld, its color: told apart by both, and the label or the tooltip names it.
	 */
	private static final class DestinationButton extends Button.Plain {
		private static final int ICON_SIZE = 10;
		private static final int ICON_INSET = 5;
		private static final int TEXT_GAP = 5;
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
			graphics.fill(getX(), getY(), getRight(), getBottom(), isHoveredOrFocused() ? HOVER : ContainerPanel.SLOT_OUTLINE);
			graphics.fill(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1, ContainerPanel.SLOT_FILL);
			if (tint != 0) {
				graphics.fill(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1, tint);
			}
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, getX() + ICON_INSET, getY() + (getHeight() - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE);
			Font font = Minecraft.getInstance().font;
			int textX = getX() + ICON_INSET + ICON_SIZE + TEXT_GAP;
			graphics.text(font, font.substrByWidth(getMessage(), getRight() - TEXT_GAP - textX).getString(), textX,
					getY() + (getHeight() - font.lineHeight) / 2 + 1, TEXT, false);
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
		// The inventory's key closes it, as it closes the game's windows, unless the name is being typed
		if (event.key() == InputConstants.KEY_RETURN || !(getFocused() instanceof EditBox box && box.isFocused()) && minecraft.options.keyInventory.matches(event)) {
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
			ScrollSounds.moved(scroll, moved);
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
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			for (boolean up : new boolean[] {true, false}) {
				if (overArrow(event.x(), event.y(), up)) {
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
					move(rowAt(event.x(), event.y()), up, event.hasShiftDown());
					return true;
				}
			}
		}
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
		ContainerPanel.draw(graphics, (width - panelWidth()) / 2, panelTop(), panelWidth(), panelHeight());
		graphics.text(font, title, rowLeft(), panelTop() + BORDER + PADDING, ContainerPanel.LABEL, false);
		extractArrows(graphics, mouseX, mouseY);
		extractHeading(graphics);
		if (rows.isEmpty()) {
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
