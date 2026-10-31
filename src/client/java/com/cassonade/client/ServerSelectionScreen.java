package com.cassonade.client;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import com.cassonade.client.WorldSelectionScreen.Tag;
import com.cassonade.client.loading.WorldLoading;
import com.cassonade.client.map.FramedPanel;
import com.cassonade.client.map.SmallText;
import com.cassonade.client.mixin.AbstractSelectionListAccessor;
import com.cassonade.client.mixin.NetworkServerEntryAccessor;
import com.cassonade.client.mixin.OnlineServerEntryAccessor;
import com.cassonade.client.mixin.ServerSelectionListAccessor;

/**
 * Replaces vanilla's server list with two panels, as the world selection: the servers on the left, each with its
 * icon, its description and whether it answers; the server selected on the right, with a large icon, its name in
 * large letters, its details and the buttons to join, edit or delete it. The first server is selected as the
 * screen opens, and the one selected stays so when coming back from editing it.
 * <p>
 * The list, the pings, the local network's search and the actions are vanilla's: its init makes the widgets, moved
 * here into the panels, and its rows are drawn here for this screen only (OnlineServerEntryMixin, NetworkServerEntryMixin).
 */
public class ServerSelectionScreen extends JoinMultiplayerScreen {
	private static final int MARGIN = WorldSelectionScreen.MARGIN;
	private static final int GAP = WorldSelectionScreen.GAP;
	private static final int INSET = WorldSelectionScreen.INSET;
	private static final int BUTTON_HEIGHT = WorldSelectionScreen.BUTTON_HEIGHT;
	private static final int ROW_ICON = WorldSelectionScreen.ROW_ICON;
	private static final int MOTD_LINES = 2;
	/** What vanilla shows for a server without an icon, the local network's worlds included. */
	private static final Identifier UNKNOWN_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_server.png");
	private static final Component LAN_WORLD = Component.translatable("lanServer.title");
	private static final Component SCANNING = Component.translatable("lanServer.scanning");
	private static final Component PLAYERS = Component.translatable("gui.cassonade.servers.players");

	private int listLeft;
	private int listWidth;
	private int detailLeft;
	private int detailWidth;
	private int panelsTop;
	private int panelsHeight;
	private int titleTop;

	public ServerSelectionScreen(Screen lastScreen) {
		super(lastScreen);
	}

	@Override
	protected void init() {
		// Vanilla makes a new list each time this screen comes back: the row selected is carried over by its place
		int selectedRow = serverSelectionList == null || serverSelectionList.getSelected() == null ? -1
				: serverSelectionList.children().indexOf(serverSelectionList.getSelected());
		super.init();
		((AbstractSelectionListAccessor) serverSelectionList).setDefaultEntryHeight(WorldSelectionScreen.ROW_HEIGHT);
		serverSelectionList.children().forEach(entry -> entry.setHeight(WorldSelectionScreen.ROW_HEIGHT));
		children().stream().filter(StringWidget.class::isInstance).map(StringWidget.class::cast).forEach(label -> label.visible = false);
		repositionElements();
		if (selectedRow >= 0 && selectedRow < serverSelectionList.children().size()) {
			serverSelectionList.setSelected(serverSelectionList.children().get(selectedRow));
		}
	}

	/** Moves vanilla's widgets: rebuilding them on a resize would start another search on the local network. */
	@Override
	protected void repositionElements() {
		int totalWidth = Math.min(width - 2 * MARGIN, WorldSelectionScreen.MAX_WIDTH);
		int titleHeight = font.lineHeight * WorldSelectionScreen.TITLE_SCALE;
		panelsHeight = Math.min(height - 2 * MARGIN - titleHeight - GAP, WorldSelectionScreen.MAX_HEIGHT);
		titleTop = (height - titleHeight - GAP - panelsHeight) / 2;
		panelsTop = titleTop + titleHeight + GAP;
		listLeft = (width - totalWidth) / 2;
		listWidth = Mth.clamp(totalWidth * 2 / 5, 150, 210);
		detailLeft = listLeft + listWidth + GAP;
		detailWidth = totalWidth - listWidth - GAP;

		// Left: the servers, then the buttons to add one, to connect directly and to go back
		int x = listLeft + INSET;
		int innerWidth = listWidth - 2 * INSET;
		int top = panelsTop + INSET;
		int bottom = panelsTop + panelsHeight - INSET;
		int actionsTop = bottom - BUTTON_HEIGHT;
		int upperTop = actionsTop - 4 - BUTTON_HEIGHT;
		serverSelectionList.updateSizeAndPosition(innerWidth, upperTop - GAP - top, x, top);
		int backWidth = Math.max(50, font.width(CommonComponents.GUI_BACK) + 16);
		place("selectServer.add", x, upperTop, innerWidth);
		place("selectServer.direct", x, actionsTop, innerWidth - backWidth - 4);
		place("gui.back", x + innerWidth - backWidth, actionsTop, backWidth);

		// Right: joining the server selected, then editing or deleting it, or refreshing the list
		x = detailLeft + INSET;
		innerWidth = detailWidth - 2 * INSET;
		int third = (innerWidth - 8) / 3;
		place("selectServer.select", x, upperTop, innerWidth);
		place("selectServer.edit", x, actionsTop, third);
		place("selectServer.delete", x + third + 4, actionsTop, third);
		place("selectServer.refresh", x + innerWidth - third, actionsTop, third);
	}

	/** Vanilla's button with this text: found by its key, as another mod may have styled it or removed it. */
	private void place(String key, int x, int y, int width) {
		for (var child : children()) {
			if (child instanceof Button button && button.getMessage().getContents() instanceof TranslatableContents text && text.getKey().equals(key)) {
				button.setPosition(x, y);
				button.setWidth(width);
				return;
			}
		}
	}

	/** The name of the server selected, for the tests. */
	public @Nullable String selectedName() {
		return serverSelectionList.getSelected() instanceof ServerSelectionList.OnlineServerEntry server ? server.getServerData().name : null;
	}

	/** Whether a list is this screen's, whose rows and background are drawn here rather than by vanilla. */
	public static boolean styles(Object list) {
		return list instanceof ServerSelectionList && ((ServerSelectionListAccessor) list).getScreen() instanceof ServerSelectionScreen;
	}

	// --- Drawing -------------------------------------------------------------------------------

	/** The loading screens' background, so that nothing jumps when joining a server. */
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		WorldLoading.extractBackground(graphics, width, height);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		selectFirstServer();
		// Rows added one by one were measured before the list needed its scrollbar
		serverSelectionList.children().forEach(entry -> entry.setWidth(serverSelectionList.getRowWidth()));
		FramedPanel.draw(graphics, listLeft, panelsTop, listWidth, panelsHeight);
		FramedPanel.draw(graphics, detailLeft, panelsTop, detailWidth, panelsHeight);
		WorldSelectionScreen.extractTitle(graphics, font, title, width / 2f, titleTop);
		super.extractRenderState(graphics, mouseX, mouseY, a);
		extractDetails(graphics, mouseX, mouseY);
	}

	/** The first server is selected as the screen opens, and again when the one selected was deleted. */
	private void selectFirstServer() {
		if (serverSelectionList.getSelected() != null) {
			return;
		}
		for (ServerSelectionList.Entry entry : serverSelectionList.children()) {
			if (!(entry instanceof ServerSelectionList.LANHeader)) {
				serverSelectionList.setSelected(entry);
				return;
			}
		}
	}

	private void extractDetails(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int left = detailLeft + INSET;
		int right = detailLeft + detailWidth - INSET;
		int top = panelsTop + INSET;
		int bottom = panelsTop + panelsHeight - INSET - 2 * BUTTON_HEIGHT - 4 - GAP;
		ServerSelectionList.Entry entry = serverSelectionList.getSelected();
		if (entry instanceof ServerSelectionList.OnlineServerEntry server) {
			ServerData data = server.getServerData();
			Identifier icon = ((OnlineServerEntryAccessor) (Object) server).getIcon().textureLocation();
			List<Tag> tags = List.of(stateTag(data));
			extractHeader(graphics, icon, Component.literal(data.name), address(data.ip), tags, data.motd, left, right, top, bottom,
					sheet(data), data.playerList, mouseX, mouseY);
		} else if (entry instanceof ServerSelectionList.NetworkServerEntry network) {
			LanServer lan = ((NetworkServerEntryAccessor) network).getServerData();
			extractHeader(graphics, UNKNOWN_ICON, Component.literal(lan.getMotd()), address(lan.getAddress()),
					List.of(Tag.neutral(LAN_WORLD)), Component.empty(), left, right, top, bottom, List.of(), List.of(), mouseX, mouseY);
		} else {
			Component pick = Component.translatable("gui.cassonade.servers.pick");
			graphics.text(font, pick, (left + right - font.width(pick)) / 2, (top + bottom - font.lineHeight) / 2, WorldSelectionScreen.MUTED, true);
		}
	}

	/** The icon, sunk in the panel, with the name, the address and the tags beside it, then the description and the sheet below. */
	private void extractHeader(GuiGraphicsExtractor graphics, Identifier texture, Component name, Component address, List<Tag> tags, Component motd,
			int left, int right, int top, int bottom, List<Component[]> sheet, List<Component> players, int mouseX, int mouseY) {
		int icon = bottom - top >= WorldSelectionScreen.LARGE_ICON + 60 ? WorldSelectionScreen.LARGE_ICON : WorldSelectionScreen.SMALL_ICON;
		InfoPanel.sunk(graphics, left, top, left + icon + 2, top + icon + 2);
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left + 1, top + 1, 0, 0, icon, icon, icon, icon);

		int textLeft = left + icon + 2 + 8;
		int y = WorldSelectionScreen.extractName(graphics, font, name, textLeft, right - textLeft, top + 2);
		SmallText.left(graphics, font, WorldSelectionScreen.ellipsize(font, address, (int) ((right - textLeft) / SmallText.scale())), textLeft, y, WorldSelectionScreen.MUTED);
		y += SmallText.lineHeight(font) + 6;
		y = WorldSelectionScreen.extractTags(graphics, font, tags, textLeft, right, y);

		y = Math.max(y, top + icon + 2) + 10;
		List<FormattedCharSequence> lines = font.split(motd, right - left);
		for (FormattedCharSequence line : lines.subList(0, Math.min(MOTD_LINES, lines.size()))) {
			if (y + font.lineHeight > bottom) {
				return;
			}
			graphics.text(font, line, left, y, WorldSelectionScreen.TEXT, true);
			y += font.lineHeight + 1;
		}
		if (!lines.isEmpty()) {
			y += 5;
		}
		int rowHeight = sheet.isEmpty() ? WorldSelectionScreen.SHEET_ROW
				: Mth.clamp((bottom - y) / sheet.size(), WorldSelectionScreen.TIGHT_SHEET_ROW, WorldSelectionScreen.SHEET_ROW);
		for (Component[] row : sheet) {
			if (y + rowHeight > bottom) {
				return;
			}
			WorldSelectionScreen.extractSheetRow(graphics, font, row[0], row[1], left, right, y, rowHeight);
			// As vanilla over the count: who is online, when the server tells
			if (row[0] == PLAYERS && !players.isEmpty() && mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + rowHeight) {
				graphics.setTooltipForNextFrame(Lists.transform(players, Component::getVisualOrderText), mouseX, mouseY);
			}
			y += rowHeight;
		}
	}

	private static List<Component[]> sheet(ServerData data) {
		List<Component[]> rows = new ArrayList<>();
		if (data.state() != ServerData.State.SUCCESSFUL && data.state() != ServerData.State.INCOMPATIBLE) {
			return rows;
		}
		if (data.players != null) {
			rows.add(new Component[] {PLAYERS, playerCount(data)});
		}
		rows.add(new Component[] {Component.translatable("gui.cassonade.servers.ping"), Component.translatable("multiplayer.status.ping", data.ping)});
		rows.add(new Component[] {Component.translatable("gui.cassonade.worlds.version"), data.version});
		return rows;
	}

	private static Component playerCount(ServerData data) {
		return Component.literal(data.players.online() + "/" + data.players.max());
	}

	/** The address, unless the player hides them, as vanilla when streaming. */
	private Component address(String address) {
		return minecraft.options.hideServerAddress ? Component.translatable("selectServer.hiddenAddress") : Component.literal(address);
	}

	/**
	 * Vanilla leaves a server whose address cannot be resolved pinging for good, with only its description telling so:
	 * that description, emptied as a ping starts, marks it as out of reach.
	 */
	private static ServerData.State state(ServerData data) {
		return data.state() == ServerData.State.PINGING && !data.motd.getString().isEmpty() ? ServerData.State.UNREACHABLE : data.state();
	}

	private static Tag stateTag(ServerData data) {
		return switch (state(data)) {
			case INITIAL, PINGING -> Tag.neutral(Component.translatable("multiplayer.status.pinging"));
			case SUCCESSFUL -> new Tag(Component.translatable("multiplayer.status.online"), 0xFF547A12, 0xFFEAF6CC, false);
			case INCOMPATIBLE -> new Tag(Component.translatable("multiplayer.status.incompatible"), 0xFF935F00, 0xFFFFE9A8, false);
			case UNREACHABLE -> new Tag(Component.translatable("gui.cassonade.servers.offline"), WorldSelectionScreen.HARDCORE_FILL,
					WorldSelectionScreen.HARDCORE_TEXT, false);
		};
	}

	private static Identifier statusSprite(ServerData data, int row) {
		return Identifier.withDefaultNamespace("server_list/" + switch (state(data)) {
			case INITIAL, PINGING -> {
				// Vanilla's animation: the bars go up and back down, each row a little behind the one above
				int frame = (int) (Util.getMillis() / 100L + row * 2 & 7L);
				yield "pinging_" + Mth.clamp(frame > 4 ? 8 - frame : frame, 1, 5);
			}
			case INCOMPATIBLE -> "incompatible";
			case UNREACHABLE -> "unreachable";
			case SUCCESSFUL -> data.ping < 150L ? "ping_5" : data.ping < 300L ? "ping_4" : data.ping < 600L ? "ping_3" : data.ping < 1000L ? "ping_2" : "ping_1";
		});
	}

	/**
	 * A server's row: its icon, which joins it or moves it in the list when clicked as in vanilla, its name, whether
	 * it answers and its description in small letters, then how many are playing in a tag.
	 */
	public void extractRow(GuiGraphicsExtractor graphics, ServerSelectionList.OnlineServerEntry entry, Identifier icon, boolean hovered, int mouseX, int mouseY) {
		ServerData data = entry.getServerData();
		int left = entry.getX();
		int top = entry.getY();
		int right = left + entry.getWidth();
		int bottom = top + entry.getHeight() - 2;
		boolean selected = serverSelectionList.getSelected() == entry;
		WorldSelectionScreen.extractCard(graphics, left, top, right, bottom, selected, hovered, icon);
		int iconX = left + WorldSelectionScreen.ROW_ICON_INSET;
		int iconY = top + WorldSelectionScreen.ROW_ICON_INSET;
		int row = serverSelectionList.children().indexOf(entry);
		if (hovered) {
			extractIconActions(graphics, entry, iconX, iconY, row, mouseX, mouseY);
		}

		int textLeft = iconX + ROW_ICON + 8;
		int textRight = right - WorldSelectionScreen.ROW_ICON_INSET;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, statusSprite(data, row), textRight - 10, iconY, 10, 8);
		graphics.text(font, WorldSelectionScreen.ellipsize(font, Component.literal(data.name), textRight - 14 - textLeft), textLeft, iconY,
				selected ? WorldSelectionScreen.GOLD : WorldSelectionScreen.TEXT, true);
		List<FormattedCharSequence> motd = font.split(data.motd, (int) ((textRight - textLeft) / SmallText.scale()));
		if (!motd.isEmpty()) {
			SmallText.left(graphics, font, motd.getFirst(), textLeft, iconY + font.lineHeight + 3,
					selected ? WorldSelectionScreen.SELECTED_MUTED : WorldSelectionScreen.MUTED);
		}
		Tag tag = data.state() == ServerData.State.SUCCESSFUL && data.players != null
				? new Tag(playerCount(data), 0xFF547A12, 0xFFEAF6CC, false) : stateTag(data);
		WorldSelectionScreen.extractSmallTag(graphics, font, tag, textLeft, iconY + ROW_ICON);
		WorldSelectionScreen.closeCard(graphics, left, top, right, bottom, selected, hovered, serverSelectionList.getY(), serverSelectionList.getBottom());
	}

	/** As vanilla: the right of the icon joins the server, its corners on the left move it up or down the list; all of it joins a server alone. */
	private void extractIconActions(GuiGraphicsExtractor graphics, ServerSelectionList.OnlineServerEntry entry, int iconX, int iconY, int row, int mouseX, int mouseY) {
		graphics.fill(iconX, iconY, iconX + ROW_ICON, iconY + ROW_ICON, WorldSelectionScreen.ICON_HOVER);
		int relX = mouseX - entry.getContentX();
		int relY = mouseY - entry.getContentY();
		boolean overJoin = entry.mouseOverRightHalf(relX, relY, ROW_ICON);
		boolean overUp = row > 0 && entry.mouseOverTopLeftQuarter(relX, relY, ROW_ICON);
		boolean overDown = row < getServers().size() - 1 && entry.mouseOverBottomLeftQuarter(relX, relY, ROW_ICON);
		// Alone in the list, nothing to move: the arrow takes the middle of the icon, as a world's
		boolean alone = getServers().size() <= 1;
		WorldSelectionScreen.extractJoinArrow(graphics, iconX + (alone ? 12 : 19), iconY + 15, overJoin ? WorldSelectionScreen.GOLD : WorldSelectionScreen.HOVER);
		if (row > 0) {
			extractMoveArrow(graphics, iconX + 7, iconY + 4, 1, overUp ? WorldSelectionScreen.GOLD : WorldSelectionScreen.HOVER);
		}
		if (row < getServers().size() - 1) {
			extractMoveArrow(graphics, iconX + 7, iconY + ROW_ICON - 5, -1, overDown ? WorldSelectionScreen.GOLD : WorldSelectionScreen.HOVER);
		}
		if (overJoin || overUp || overDown) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	/** A small arrow, its tip at (x, y) and widening in the direction given, outlined as the arrow to join. */
	private static void extractMoveArrow(GuiGraphicsExtractor graphics, int x, int y, int direction, int color) {
		for (int i = 0; i <= 4; i++) {
			int rowY = y + i * direction;
			graphics.fill(x - i - 1, rowY - 1, x + i + 2, rowY + 2, 0xFF160608);
		}
		for (int i = 0; i <= 4; i++) {
			int rowY = y + i * direction;
			graphics.fill(x - i, rowY, x + i + 1, rowY + 1, color);
		}
	}

	/** A world on the local network: its description, its address in small letters, and a tag telling where it comes from. */
	public void extractRow(GuiGraphicsExtractor graphics, ServerSelectionList.NetworkServerEntry entry, boolean hovered) {
		LanServer lan = ((NetworkServerEntryAccessor) entry).getServerData();
		int left = entry.getX();
		int top = entry.getY();
		int right = left + entry.getWidth();
		int bottom = top + entry.getHeight() - 2;
		boolean selected = serverSelectionList.getSelected() == entry;
		WorldSelectionScreen.extractCard(graphics, left, top, right, bottom, selected, hovered, UNKNOWN_ICON);
		int iconY = top + WorldSelectionScreen.ROW_ICON_INSET;
		int textLeft = left + WorldSelectionScreen.ROW_ICON_INSET + ROW_ICON + 8;
		int textWidth = right - WorldSelectionScreen.ROW_ICON_INSET - textLeft;
		graphics.text(font, WorldSelectionScreen.ellipsize(font, Component.literal(lan.getMotd()), textWidth), textLeft, iconY,
				selected ? WorldSelectionScreen.GOLD : WorldSelectionScreen.TEXT, true);
		SmallText.left(graphics, font, WorldSelectionScreen.ellipsize(font, address(lan.getAddress()), (int) (textWidth / SmallText.scale())),
				textLeft, iconY + font.lineHeight + 3, selected ? WorldSelectionScreen.SELECTED_MUTED : WorldSelectionScreen.MUTED);
		WorldSelectionScreen.extractSmallTag(graphics, font, Tag.neutral(LAN_WORLD), textLeft, iconY + ROW_ICON);
		WorldSelectionScreen.closeCard(graphics, left, top, right, bottom, selected, hovered, serverSelectionList.getY(), serverSelectionList.getBottom());
	}

	/** The search on the local network, centered in its row, as wide as the panel lets it. */
	public void extractScanning(GuiGraphicsExtractor graphics, ServerSelectionList.Entry entry) {
		int middle = entry.getX() + entry.getWidth() / 2;
		List<FormattedCharSequence> lines = font.split(SCANNING, entry.getWidth() - 8);
		int y = entry.getY() + (entry.getHeight() - 2 - (lines.size() + 1) * (font.lineHeight + 1)) / 2;
		for (FormattedCharSequence line : lines) {
			graphics.text(font, line, middle - font.width(line) / 2, y, WorldSelectionScreen.MUTED, true);
			y += font.lineHeight + 1;
		}
		String dots = LoadingDotsText.get(Util.getMillis());
		graphics.text(font, dots, middle - font.width(dots) / 2, y, WorldSelectionScreen.MUTED, true);
	}
}
