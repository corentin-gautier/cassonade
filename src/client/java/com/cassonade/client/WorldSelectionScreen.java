package com.cassonade.client;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelSummary;

import com.cassonade.client.map.FramedPanel;
import com.cassonade.client.map.SmallText;
import com.cassonade.client.mixin.AbstractSelectionListAccessor;
import com.cassonade.client.mixin.WorldListEntryAccessor;

/**
 * Replaces vanilla's world list with two panels: the worlds on the left, each with its icon, when it was last
 * played and its game mode; the world selected on the right, with a large icon, its name in large letters, its
 * settings as tags and the buttons to play, edit, re-create or delete it. The most recent world is selected
 * as the screen opens, so its panel is never empty.
 * <p>
 * Loading the worlds, searching and the actions are vanilla's (WorldSelectionList): only the layout and the
 * drawing change, the rows being drawn here for this screen only (WorldListEntryMixin).
 */
public class WorldSelectionScreen extends SelectWorldScreen {
	private static final int MARGIN = 8;
	private static final int GAP = 6;
	private static final int MAX_WIDTH = 520;
	private static final int MAX_HEIGHT = 320;
	private static final int INSET = FramedPanel.BORDER + 6;
	private static final int BUTTON_HEIGHT = 20;
	private static final int TITLE_SCALE = 2;
	public static final int ROW_HEIGHT = 46;
	/** Right of the rows: the scrollbar and a gap before it. */
	public static final int SCROLLBAR_SPACE = 9;
	private static final int WOOD_DARK = 0xFF4D2E28;
	private static final int ROW_ICON = 32;
	/** From the row's edge to its icon: its border and the padding around the icon. */
	public static final int ROW_ICON_INSET = 6;
	/** The world's icon in the right panel: its 64 pixels, each drawn on one GUI pixel. */
	private static final int LARGE_ICON = 64;
	private static final int SMALL_ICON = 48;
	/** The sheet's rows, tighter when the panel is short so that they all fit. */
	private static final int SHEET_ROW = 19;
	private static final int TIGHT_SHEET_ROW = 14;
	/** A letter's height with its shadow, without the descenders: what is centered in the tags and the sheet's rows. */
	private static final int CAP_HEIGHT = 8;
	// The theme's colors (tools/gui_theme.py), as the quest journal's window
	private static final int GOLD = 0xFFF6DA8A;
	private static final int TEXT = 0xFFE8DABC;
	private static final int MUTED = 0xFFA38883;
	private static final int RED = 0xFFF26456;
	private static final int SEAM = 0xFF1C1109;
	private static final int FILL = 0xFF2E1D1C;
	private static final int HOVERED_FILL = 0xFF3B2825;
	// The world selected is a wood plank, as the windows' panels, lit on its top edge
	private static final int SELECTED_FILL = 0xFF6E4838;
	private static final int SELECTED_EDGE = 0xFF865E46;
	private static final int SELECTED_OUTLINE = 0xFF160608;
	private static final int SELECTED_MUTED = 0xFFD2BBA6;
	private static final int HOVER = 0xFFEBD8C7;
	private static final int TAG_OUTLINE = 0xFF160608;
	private static final int HARDCORE_FILL = 0xFF4E0B12;
	private static final int HARDCORE_TEXT = 0xFFFFD5CF;
	private static final int ICON_HOVER = 0xA0160608;
	private static final Identifier HARDCORE_HEART = Identifier.withDefaultNamespace("hud/heart/hardcore_full");

	/** A game mode's or a setting's tag: its background and its text. */
	private record Tag(Component text, int fill, int color, boolean heart) {
		static Tag neutral(Component text) {
			return new Tag(text, 0xFF6E4838, TEXT, false);
		}
	}

	private @Nullable WorldSelectionList worlds;
	private @Nullable Button playButton;
	private @Nullable Button editButton;
	private @Nullable Button recreateButton;
	private @Nullable Button deleteButton;
	private @Nullable LevelSummary selected;
	private int listLeft;
	private int listWidth;
	private int detailLeft;
	private int detailWidth;
	private int panelsTop;
	private int panelsHeight;
	private int titleTop;
	private final Map<String, Long> playTimes = new HashMap<>();

	public WorldSelectionScreen(Screen lastScreen) {
		super(lastScreen);
	}

	@Override
	protected void init() {
		int totalWidth = Math.min(width - 2 * MARGIN, MAX_WIDTH);
		int titleHeight = font.lineHeight * TITLE_SCALE;
		panelsHeight = Math.min(height - 2 * MARGIN - titleHeight - GAP, MAX_HEIGHT);
		titleTop = (height - titleHeight - GAP - panelsHeight) / 2;
		panelsTop = titleTop + titleHeight + GAP;
		listLeft = (width - totalWidth) / 2;
		listWidth = Mth.clamp(totalWidth * 2 / 5, 150, 210);
		detailLeft = listLeft + listWidth + GAP;
		detailWidth = totalWidth - listWidth - GAP;

		// Left: the search, the worlds, then the buttons to create one and to go back
		int x = listLeft + INSET;
		int innerWidth = listWidth - 2 * INSET;
		int top = panelsTop + INSET;
		int bottom = panelsTop + panelsHeight - INSET;
		searchBox = new EditBox(font, x, top, innerWidth, BUTTON_HEIGHT, searchBox, Component.translatable("selectWorld.search"));
		searchBox.setResponder(value -> {
			if (worlds != null) {
				worlds.updateFilter(value);
			}
		});
		searchBox.setHint(Component.translatable("gui.selectWorld.search").setStyle(EditBox.SEARCH_HINT_STYLE));
		addRenderableWidget(searchBox);

		int listTop = top + BUTTON_HEIGHT + GAP;
		int listBottom = bottom - BUTTON_HEIGHT - GAP;
		worlds = new WorldSelectionList.Builder(minecraft, this)
				.width(innerWidth)
				.height(listBottom - listTop)
				.filter(searchBox.getValue())
				.oldList(worlds)
				.onEntrySelect(this::updateButtonStatus)
				.onEntryInteract(WorldSelectionList.WorldListEntry::joinWorld)
				.build();
		// Taller rows than vanilla's, for the icon to breathe; the rows already there get the height too
		((AbstractSelectionListAccessor) worlds).setDefaultEntryHeight(ROW_HEIGHT);
		worlds.children().forEach(entry -> entry.setHeight(ROW_HEIGHT));
		worlds.updateSizeAndPosition(innerWidth, listBottom - listTop, x, listTop);
		addRenderableWidget(worlds);

		WorldSelectionList list = worlds;
		Component back = CommonComponents.GUI_BACK;
		int backWidth = Math.max(50, font.width(back) + 16);
		addRenderableWidget(Button.builder(Component.translatable("gui.cassonade.worlds.new"),
				button -> CreateWorldScreen.openFresh(minecraft, list::returnToScreen))
				.bounds(x, bottom - BUTTON_HEIGHT, innerWidth - backWidth - 4, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(back, button -> onClose())
				.bounds(x + innerWidth - backWidth, bottom - BUTTON_HEIGHT, backWidth, BUTTON_HEIGHT).build());

		// Right: playing the world selected, then editing, re-creating or deleting it
		x = detailLeft + INSET;
		innerWidth = detailWidth - 2 * INSET;
		int third = (innerWidth - 8) / 3;
		int actionsTop = bottom - BUTTON_HEIGHT;
		playButton = addRenderableWidget(Button.builder(LevelSummary.PLAY_WORLD,
				button -> list.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::joinWorld))
				.bounds(x, actionsTop - 4 - BUTTON_HEIGHT, innerWidth, BUTTON_HEIGHT).build());
		editButton = addRenderableWidget(Button.builder(Component.translatable("selectWorld.edit"),
				button -> list.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::editWorld))
				.bounds(x, actionsTop, third, BUTTON_HEIGHT).build());
		recreateButton = addRenderableWidget(Button.builder(Component.translatable("selectWorld.recreate"),
				button -> list.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::recreateWorld))
				.bounds(x + third + 4, actionsTop, third, BUTTON_HEIGHT).build());
		deleteButton = addRenderableWidget(Button.builder(Component.translatable("selectWorld.delete"),
				button -> list.getSelectedOpt().ifPresent(WorldSelectionList.WorldListEntry::deleteWorld))
				.bounds(x + innerWidth - third, actionsTop, third, BUTTON_HEIGHT).build());
		updateButtonStatus(list.getSelectedOpt().map(WorldSelectionList.WorldListEntry::getLevelSummary).orElse(null));
	}

	@Override
	protected void repositionElements() {
		rebuildWidgets();
	}

	@Override
	public void updateButtonStatus(@Nullable LevelSummary summary) {
		selected = summary;
		if (playButton == null || editButton == null || recreateButton == null || deleteButton == null) {
			return;
		}
		playButton.setMessage(summary == null ? LevelSummary.PLAY_WORLD : summary.primaryActionMessage());
		playButton.active = summary != null && summary.primaryActionActive();
		editButton.active = summary != null && summary.canEdit();
		recreateButton.active = summary != null && summary.canRecreate();
		deleteButton.active = summary != null && summary.canDelete();
		boolean fixing = summary != null && summary.requiresFileFixing();
		editButton.setTooltip(fixing ? Tooltip.create(Component.translatable("selectWorld.requiresFileFixingTooltip.edit")) : null);
		playButton.setTooltip(fixing ? Tooltip.create(Component.translatable("selectWorld.requiresFileFixingTooltip.play")) : null);
		recreateButton.setTooltip(fixing ? Tooltip.create(Component.translatable("selectWorld.requiresFileFixingTooltip.recreate")) : null);
	}

	@Override
	public void removed() {
		if (worlds != null) {
			worlds.children().forEach(WorldSelectionList.Entry::close);
		}
	}

	/** The name of the world selected, for the tests. */
	public @Nullable String selectedName() {
		return selected == null ? null : selected.getLevelName();
	}

	/** Whether a list is this screen's, whose rows and background are drawn here rather than by vanilla. */
	public static boolean styles(Object list) {
		return list instanceof WorldSelectionList worlds && worlds.getScreen() instanceof WorldSelectionScreen;
	}

	// --- Drawing -------------------------------------------------------------------------------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		selectFirstWorld();
		FramedPanel.draw(graphics, listLeft, panelsTop, listWidth, panelsHeight);
		FramedPanel.draw(graphics, detailLeft, panelsTop, detailWidth, panelsHeight);
		graphics.pose().pushMatrix();
		graphics.pose().translate(width / 2f, titleTop);
		graphics.pose().scale(TITLE_SCALE, TITLE_SCALE);
		graphics.text(font, title, -font.width(title) / 2, 0, GOLD, true);
		graphics.pose().popMatrix();
		super.extractRenderState(graphics, mouseX, mouseY, a);

		if (worlds != null && worlds.children().isEmpty()) {
			Component none = Component.translatable("gui.cassonade.worlds.none");
			graphics.text(font, none, worlds.getX() + (worlds.getWidth() - font.width(none)) / 2, worlds.getY() + 8, MUTED, true);
		}
		extractDetails(graphics);
	}

	/** The most recent world is selected as the list fills, and again when a search leaves none selected. */
	private void selectFirstWorld() {
		if (worlds == null || worlds.getSelected() != null) {
			return;
		}
		for (WorldSelectionList.Entry entry : worlds.children()) {
			if (entry instanceof WorldSelectionList.WorldListEntry world) {
				worlds.setSelected(world);
				return;
			}
		}
	}

	private void extractDetails(GuiGraphicsExtractor graphics) {
		int left = detailLeft + INSET;
		int right = detailLeft + detailWidth - INSET;
		int top = panelsTop + INSET;
		int bottom = panelsTop + panelsHeight - INSET - 2 * BUTTON_HEIGHT - 4 - GAP;
		LevelSummary summary = selected;
		WorldSelectionList.WorldListEntry entry = worlds == null ? null : worlds.getSelectedOpt().orElse(null);
		if (summary == null || entry == null) {
			Component pick = Component.translatable("gui.cassonade.worlds.pick");
			graphics.text(font, pick, (left + right - font.width(pick)) / 2, (top + bottom - font.lineHeight) / 2, MUTED, true);
			return;
		}

		// The icon, sunk in the panel as a slot, with the name and when it was played beside it
		int icon = bottom - top >= LARGE_ICON + 60 ? LARGE_ICON : SMALL_ICON;
		InfoPanel.sunk(graphics, left, top, left + icon + 2, top + icon + 2);
		Identifier texture = ((WorldListEntryAccessor) (Object) entry).getIcon().textureLocation();
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left + 1, top + 1, 0, 0, icon, icon, icon, icon);

		int textLeft = left + icon + 2 + 8;
		int textWidth = right - textLeft;
		Component name = Component.literal(summary.getLevelName());
		int y = top + 2;
		if (font.width(name) * TITLE_SCALE <= textWidth) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(textLeft, y);
			graphics.pose().scale(TITLE_SCALE, TITLE_SCALE);
			graphics.text(font, name, 0, 0, GOLD, true);
			graphics.pose().popMatrix();
			y += font.lineHeight * TITLE_SCALE + 3;
		} else {
			List<FormattedCharSequence> lines = font.split(name, textWidth);
			for (FormattedCharSequence line : lines.subList(0, Math.min(2, lines.size()))) {
				graphics.text(font, line, textLeft, y, GOLD, true);
				y += font.lineHeight + 1;
			}
			y += 2;
		}
		Component played = lastPlayed(summary.getLastPlayed());
		if (played != null) {
			SmallText.left(graphics, font, played, textLeft, y, MUTED);
			y += SmallText.lineHeight(font) + 6;
		}
		y = extractTags(graphics, tags(summary), textLeft, right, y);

		// Below, a sheet: when it was played, for how long, its version and folder, then what keeps it from opening
		y = Math.max(y, top + icon + 2) + 10;
		List<Component[]> sheet = sheet(summary);
		int rowHeight = sheet.isEmpty() ? SHEET_ROW : Mth.clamp((bottom - y) / sheet.size(), TIGHT_SHEET_ROW, SHEET_ROW);
		for (Component[] row : sheet) {
			if (y + rowHeight > bottom) {
				return;
			}
			graphics.fill(left, y + rowHeight - 1, right, y + rowHeight, SEAM);
			int textY = y + (rowHeight - 1 - CAP_HEIGHT) / 2;
			graphics.text(font, row[0], left, textY, MUTED, true);
			FormattedCharSequence value = ellipsize(font, row[1], right - left - font.width(row[0]) - 12);
			graphics.text(font, value, right - font.width(value), textY, TEXT, true);
			y += rowHeight;
		}
		y += 6;
		Component problem = problem(summary);
		if (problem != null) {
			for (FormattedCharSequence line : font.split(problem, right - left)) {
				if (y + font.lineHeight > bottom) {
					break;
				}
				graphics.text(font, line, left, y, RED, true);
				y += font.lineHeight + 2;
			}
		}
	}

	private List<Component[]> sheet(LevelSummary summary) {
		List<Component[]> rows = new ArrayList<>();
		if (summary instanceof LevelSummary.CorruptedLevelSummary || summary instanceof LevelSummary.SymlinkLevelSummary) {
			return rows;
		}
		Locale locale = locale();
		if (summary.getLastPlayed() > 0) {
			DateTimeFormatter format = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale);
			rows.add(new Component[] {Component.translatable("gui.cassonade.worlds.last_played"),
					Component.literal(format.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(summary.getLastPlayed()), ZoneId.systemDefault())))});
		}
		long ticks = playTime(summary.getLevelId());
		if (ticks > 0) {
			long minutes = Math.max(1, ticks / 20 / 60);
			rows.add(new Component[] {Component.translatable("gui.cassonade.worlds.play_time"), minutes < 60
					? Component.translatable("gui.cassonade.worlds.minutes", minutes)
					: Component.translatable("gui.cassonade.worlds.hours", minutes / 60, String.format(locale, "%02d", minutes % 60))});
		}
		rows.add(new Component[] {Component.translatable("gui.cassonade.worlds.version"), summary.getWorldVersionName()});
		if (!summary.getLevelId().equals(summary.getLevelName())) {
			rows.add(new Component[] {Component.translatable("gui.cassonade.worlds.folder"), Component.literal(summary.getLevelId())});
		}
		return rows;
	}

	private static @Nullable Component problem(LevelSummary summary) {
		if (summary instanceof LevelSummary.CorruptedLevelSummary || summary instanceof LevelSummary.SymlinkLevelSummary) {
			return summary.getInfo();
		}
		return summary.isLocked() ? Component.translatable("selectWorld.locked")
				: summary.requiresManualConversion() ? Component.translatable("selectWorld.conversion.tooltip")
				: !summary.isCompatible() ? Component.translatable("selectWorld.incompatible.tooltip")
				: summary.isDowngrade() ? Component.translatable("selectWorld.tooltip.fromNewerVersion1")
				: null;
	}

	/**
	 * The time played in a world, in ticks, from the player's statistics saved in it: those of this profile, or
	 * the longest when the world was played under another one. Read once per world while the screen is open.
	 */
	private long playTime(String levelId) {
		return playTimes.computeIfAbsent(levelId, id -> {
			Path stats = minecraft.getLevelSource().getBaseDir().resolve(id).resolve(LevelResource.PLAYER_STATS_DIR.id());
			Path own = stats.resolve(minecraft.getUser().getProfileId() + ".json");
			long longest = 0;
			try (Stream<Path> files = Files.isRegularFile(own) ? Stream.of(own) : Files.list(stats)) {
				for (Path file : (Iterable<Path>) files::iterator) {
					longest = Math.max(longest, playTime(file));
				}
			} catch (IOException e) {
				// No statistics yet: the world was never saved with a player in it
			}
			return longest;
		});
	}

	private static long playTime(Path file) {
		try (Reader reader = Files.newBufferedReader(file)) {
			JsonObject custom = GsonHelper.getAsJsonObject(GsonHelper.getAsJsonObject(JsonParser.parseReader(reader).getAsJsonObject(), "stats",
					new JsonObject()), "minecraft:custom", new JsonObject());
			return GsonHelper.getAsLong(custom, "minecraft:play_time", 0);
		} catch (IOException | RuntimeException e) {
			return 0;
		}
	}

	private static Locale locale() {
		return Locale.forLanguageTag(Minecraft.getInstance().getLanguageManager().getSelected().replace('_', '-'));
	}

	private static List<Tag> tags(LevelSummary summary) {
		List<Tag> tags = new ArrayList<>();
		if (summary.isHardcore()) {
			tags.add(new Tag(Component.translatable("selectWorld.gameMode.hardcore"), HARDCORE_FILL, HARDCORE_TEXT, true));
		} else {
			tags.add(modeTag(summary));
			tags.add(Tag.neutral(summary.getSettings().difficultySettings().difficulty().getDisplayName()));
		}
		if (summary.hasCommands()) {
			tags.add(Tag.neutral(Component.translatable("selectWorld.allowCommands")));
		}
		if (summary.isExperimental()) {
			tags.add(new Tag(Component.translatable("selectWorld.experimental"), 0xFF935F00, 0xFFFFE9A8, false));
		}
		return tags;
	}

	private static Tag modeTag(LevelSummary summary) {
		Component mode = Component.translatable("selectWorld.gameMode." + summary.getGameMode().getName());
		return switch (summary.getGameMode()) {
			case SURVIVAL -> new Tag(mode, 0xFF547A12, 0xFFEAF6CC, false);
			case CREATIVE -> new Tag(mode, 0xFF935F00, 0xFFFFE9A8, false);
			case ADVENTURE -> new Tag(mode, 0xFF58267F, 0xFFEBD6FF, false);
			case SPECTATOR -> new Tag(mode, 0xFF4A4858, 0xFFDCDAE8, false);
		};
	}

	/** Draws the tags in a row from left, on the next row when one would go past right. Returns the y below them. */
	private int extractTags(GuiGraphicsExtractor graphics, List<Tag> tags, int left, int right, int y) {
		int height = CAP_HEIGHT + 6;
		int x = left;
		for (Tag tag : tags) {
			int width = font.width(tag.text()) + 8 + (tag.heart() ? 11 : 0);
			if (x > left && x + width > right) {
				x = left;
				y += height + 3;
			}
			graphics.fill(x, y, x + width, y + height, TAG_OUTLINE);
			graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, tag.fill());
			graphics.fill(x + 1, y + 1, x + width - 1, y + 2, lighter(tag.fill()));
			int textX = x + 4;
			if (tag.heart()) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HARDCORE_HEART, textX - 1, y + (height - 9) / 2 + 1, 9, 9);
				textX += 11;
			}
			graphics.text(font, tag.text(), textX, y + (height - CAP_HEIGHT) / 2, tag.color(), true);
			x += width + 3;
		}
		return y + height;
	}

	private static int lighter(int color) {
		int r = Math.min(255, ((color >> 16) & 0xFF) + 32);
		int g = Math.min(255, ((color >> 8) & 0xFF) + 32);
		int b = Math.min(255, (color & 0xFF) + 32);
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	/** When the world was last played: minutes or hours ago today, yesterday, days ago this week, else its date. */
	static @Nullable Component lastPlayed(long millis) {
		if (millis <= 0) {
			return null;
		}
		Instant then = Instant.ofEpochMilli(millis);
		Instant now = Instant.now();
		ZoneId zone = ZoneId.systemDefault();
		long days = ChronoUnit.DAYS.between(LocalDate.ofInstant(then, zone), LocalDate.ofInstant(now, zone));
		long minutes = Math.max(0, Duration.between(then, now).toMinutes());
		if (days <= 0) {
			if (minutes < 1) {
				return Component.translatable("gui.cassonade.worlds.played.now");
			}
			return minutes < 60 ? Component.translatable("gui.cassonade.worlds.played.minutes", minutes)
					: Component.translatable("gui.cassonade.worlds.played.hours", minutes / 60);
		}
		if (days == 1) {
			return Component.translatable("gui.cassonade.worlds.played.yesterday");
		}
		if (days < 7) {
			return Component.translatable("gui.cassonade.worlds.played.days", days);
		}
		DateTimeFormatter format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale());
		return Component.translatable("gui.cassonade.worlds.played.date", format.format(ZonedDateTime.ofInstant(then, zone)));
	}

	/**
	 * A row of the list: the world's icon, which joins it when clicked as in vanilla, its name, when it was
	 * played, and its game mode in small letters.
	 */
	public static void extractRow(GuiGraphicsExtractor graphics, WorldSelectionList.WorldListEntry entry, Identifier icon,
			boolean selected, boolean hovered, int mouseX, int mouseY, int listTop, int listBottom) {
		Font font = Minecraft.getInstance().font;
		LevelSummary summary = entry.getLevelSummary();
		int left = entry.getX();
		int top = entry.getY();
		int right = left + entry.getWidth();
		int bottom = top + entry.getHeight() - 2;
		// Hovered: a cream outline, as everything hovered in the mod
		int border = hovered ? HOVER : selected ? SELECTED_OUTLINE : SEAM;
		graphics.fill(left, top, right, bottom, border);
		graphics.fill(left + 1, top + 1, right - 1, bottom - 1, selected ? SELECTED_FILL : hovered ? HOVERED_FILL : FILL);
		if (selected) {
			graphics.fill(left + 1, top + 1, right - 1, top + 2, SELECTED_EDGE);
		}

		int iconX = left + ROW_ICON_INSET;
		int iconY = top + ROW_ICON_INSET;
		graphics.fill(iconX - 1, iconY - 1, iconX + ROW_ICON + 1, iconY + ROW_ICON + 1, SEAM);
		graphics.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0, 0, ROW_ICON, ROW_ICON, ROW_ICON, ROW_ICON);
		if (hovered) {
			// As vanilla: a click on the icon joins the world, or tells why it cannot
			boolean overIcon = mouseX >= iconX && mouseX < iconX + ROW_ICON && mouseY >= iconY && mouseY < iconY + ROW_ICON;
			graphics.fill(iconX, iconY, iconX + ROW_ICON, iconY + ROW_ICON, ICON_HOVER);
			if (summary.primaryActionActive()) {
				extractJoinArrow(graphics, iconX + 12, iconY + 15, overIcon ? GOLD : HOVER);
			} else {
				extractErrorMark(graphics, iconX + 14, iconY + 7, overIcon ? HARDCORE_TEXT : RED);
			}
		}

		// The name, when it was played in small letters, then the game mode's tag, as tall together as the icon
		int textLeft = iconX + ROW_ICON + 8;
		int textWidth = right - ROW_ICON_INSET - textLeft;
		Component name = Component.literal(summary.getLevelName());
		graphics.text(font, ellipsize(font, name, textWidth), textLeft, iconY, selected ? GOLD : TEXT, true);
		Component played = lastPlayed(summary.getLastPlayed());
		if (played != null) {
			SmallText.left(graphics, font, played, textLeft, iconY + font.lineHeight + 3, selected ? SELECTED_MUTED : MUTED);
		}
		Tag mode = summary.isHardcore() ? new Tag(Component.translatable("selectWorld.gameMode.hardcore"), HARDCORE_FILL, HARDCORE_TEXT, false)
				: modeTag(summary);
		int tagHeight = SmallText.lineHeight(font) + 4;
		int tagWidth = SmallText.width(font, mode.text()) + 6;
		int tagTop = iconY + ROW_ICON - tagHeight;
		graphics.fill(textLeft, tagTop, textLeft + tagWidth, tagTop + tagHeight, TAG_OUTLINE);
		graphics.fill(textLeft + 1, tagTop + 1, textLeft + tagWidth - 1, tagTop + tagHeight - 1, mode.fill());
		SmallText.left(graphics, font, mode.text(), textLeft + 3, tagTop + (tagHeight - CAP_HEIGHT * SmallText.scale()) / 2f, mode.color());

		// A card cut by the edge of the list is closed there by its border, rather than left open
		if (top < listTop) {
			graphics.fill(left, listTop, right, listTop + 1, border);
		}
		if (bottom > listBottom) {
			graphics.fill(left, listBottom - 1, right, listBottom, border);
		}
	}

	/** The arrow to join a world, pointing right from x and centered on y, outlined so that it reads on any icon. */
	private static void extractJoinArrow(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x - 1, y - 9, x, y + 10, SELECTED_OUTLINE);
		for (int i = 0; i <= 9; i++) {
			graphics.fill(x + i, y - 9 + i, x + i + 1, y + 10 - i, SELECTED_OUTLINE);
		}
		for (int i = 0; i <= 8; i++) {
			graphics.fill(x + i, y - 8 + i, x + i + 1, y + 9 - i, color);
		}
	}

	/** The exclamation mark of a world that cannot be joined, from (x, y), outlined as the arrow to join one. */
	private static void extractErrorMark(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x - 1, y - 1, x + 5, y + 12, SELECTED_OUTLINE);
		graphics.fill(x - 1, y + 13, x + 5, y + 19, SELECTED_OUTLINE);
		graphics.fill(x, y, x + 4, y + 11, color);
		graphics.fill(x, y + 14, x + 4, y + 18, color);
	}

	/** The list's scrollbar: a groove sunk as the slots, its handle a plank of wood, outlined in cream when hovered. */
	public static void extractScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom, int width, double scrolled,
			int handleHeight, boolean hovered) {
		// From the first card's top, 2 pixels into the list, to the list's bottom, where the last card ends
		int grooveTop = top + 2;
		int grooveBottom = bottom;
		graphics.fill(x, grooveTop, x + width, grooveBottom, SEAM);
		graphics.fill(x + 1, grooveTop + 1, x + width - 1, grooveBottom - 1, FILL);
		handleHeight = Math.min(handleHeight, grooveBottom - grooveTop);
		int handleTop = grooveTop + (int) Math.round(scrolled * (grooveBottom - grooveTop - handleHeight));
		int handleBottom = handleTop + handleHeight;
		graphics.fill(x, handleTop, x + width, handleBottom, hovered ? HOVER : SELECTED_OUTLINE);
		graphics.fill(x + 1, handleTop + 1, x + width - 1, handleBottom - 1, SELECTED_FILL);
		graphics.fill(x + 1, handleTop + 1, x + width - 1, handleTop + 2, SELECTED_EDGE);
		graphics.fill(x + 1, handleBottom - 2, x + width - 1, handleBottom - 1, WOOD_DARK);
		// Three notches in the middle of the handle, to grip it
		int middle = (handleTop + handleBottom) / 2;
		for (int i = -1; i <= 1; i++) {
			int y = middle + i * 2;
			graphics.fill(x + 2, y, x + width - 2, y + 1, WOOD_DARK);
		}
	}

	private static FormattedCharSequence ellipsize(Font font, Component text, int width) {
		if (font.width(text) <= width) {
			return text.getVisualOrderText();
		}
		Component ellipsis = Component.literal("…");
		return Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(text, width - font.width(ellipsis)), ellipsis));
	}
}
