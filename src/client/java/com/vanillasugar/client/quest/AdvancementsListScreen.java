package com.vanillasugar.client.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import com.vanillasugar.client.VanillaSugarClient;
import com.vanillasugar.client.quest.AdvancementCatalog.Category;
import com.vanillasugar.client.quest.AdvancementCatalog.Entry;

/**
 * Replaces vanilla's advancement tree with a book like the quest journal (see Book): the categories
 * on the left page, their advancements on the right one, each with its description and progress.
 * A click on one with several requirements shows those left to meet, such as the biomes still to
 * visit. The list can show them all, those left to make, or those made.
 */
public class AdvancementsListScreen extends Screen implements ClientAdvancements.Listener {
	private enum Filter {
		ALL, TODO, DONE;

		boolean accepts(Entry entry) {
			return switch (this) {
				case ALL -> true;
				case TODO -> !entry.done();
				case DONE -> entry.done();
			};
		}
	}

	private static final int CATEGORY_HEIGHT = 22;
	private static final int ENTRY_GAP = 10;
	/** Around the advancement opened on its details, inside its background. */
	private static final int OPEN_PADDING = Book.SELECTION_PADDING;
	private static final int SCROLL_STEP = 24;
	private static final Component LIST_SEPARATOR = Component.literal(", ");

	/** The category and filter shown last, kept while the game runs. */
	private static @Nullable Identifier lastCategory;
	private static Filter lastFilter = Filter.ALL;

	private final @Nullable Screen lastScreen;
	private Book.Layout book = Book.Layout.centered(0, 0);
	private List<Category> categories = List.of();
	private int selected;
	private Filter filter = lastFilter;
	private double categoriesScroll;
	private double scroll;
	private @Nullable Identifier expanded;
	private final Book.Links links = new Book.Links();

	public AdvancementsListScreen(@Nullable Screen lastScreen) {
		super(Component.translatable("gui.advancements"));
		this.lastScreen = lastScreen;
	}

	@Override
	protected void init() {
		book = Book.Layout.centered(width, height);
		// Calls onAdvancementsUpdated at once, which fills the list
		minecraft.getConnection().getAdvancements().setListener(this);
	}

	@Override
	public void removed() {
		if (minecraft.getConnection() != null) {
			minecraft.getConnection().getAdvancements().setListener(null);
		}
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(lastScreen);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// Dimmed like the vanilla book and the inventories rather than tiled with the menu texture
	@Override
	public boolean isInGameUi() {
		return true;
	}

	@Override
	public void onAdvancementsUpdated() {
		categories = AdvancementCatalog.categories(minecraft);
		selected = 0;
		for (int i = 0; i < categories.size(); i++) {
			if (categories.get(i).root().id().equals(lastCategory)) {
				selected = i;
			}
		}
	}

	@Override
	public void onAdvancementsCleared() {
		categories = List.of();
	}

	@Override
	public void onSelectedTabChanged(@Nullable AdvancementHolder selectedTab) {
	}

	// --- Layout --------------------------------------------------------------------------------

	private int filtersTop() {
		return book.pagesTop() + Book.PADDING + Book.headingHeight(font, title, book.leftPageRight() - book.leftPageLeft()) + 2
				+ Book.smallLine(font) + 14;
	}

	private int categoriesTop() {
		return filtersTop() + Book.tagHeight(font) + 14;
	}

	private int categoriesBottom() {
		return book.pagesBottom() - Book.PADDING;
	}

	private @Nullable Category category() {
		return selected < categories.size() ? categories.get(selected) : null;
	}

	private List<Entry> shownEntries() {
		Category category = category();
		return category == null ? List.of() : category.entries().stream().filter(filter::accepts).toList();
	}

	private int textWidth() {
		return book.rightPageRight() - book.rightPageLeft() - Book.ICON_SIZE - 6;
	}

	private static boolean hasDetails(Entry entry) {
		return entry.requirements() > 1 && !entry.secret();
	}

	private List<FormattedCharSequence> description(Entry entry) {
		Component description = entry.secret() ? Component.translatable("gui.vanillasugar.advancements.secret.description")
				: entry.display().description();
		return Book.splitSmall(font, description, textWidth());
	}

	/** The lines shown under an advancement opened with a click: the requirements left, then those met. */
	private List<FormattedCharSequence> details(Entry entry) {
		if (!hasDetails(entry) || !entry.id().equals(expanded)) {
			return List.of();
		}
		List<Component> remaining = new ArrayList<>();
		entry.remainingCriteria().forEach(criterion -> remaining.add(AdvancementCatalog.criterionName(criterion)));
		List<Component> completed = new ArrayList<>();
		entry.completedCriteria().forEach(criterion -> completed.add(AdvancementCatalog.criterionName(criterion)));
		List<FormattedCharSequence> lines = new ArrayList<>();
		if (!remaining.isEmpty()) {
			lines.addAll(Book.splitSmall(font, Component.translatable("gui.vanillasugar.advancements.remaining",
					ComponentUtils.formatList(remaining, LIST_SEPARATOR)).withColor(Book.INK), textWidth()));
		}
		if (!completed.isEmpty()) {
			lines.addAll(Book.splitSmall(font, Component.translatable("gui.vanillasugar.advancements.completed",
					ComponentUtils.formatList(completed, LIST_SEPARATOR)).withColor(Book.GREEN_INK), textWidth()));
		}
		return lines;
	}

	private int entryHeight(Entry entry) {
		int height = Book.ICON_ROW + 2 + description(entry).size() * Book.smallLine(font);
		if (hasDetails(entry)) {
			height += 3 + Book.smallLine(font);
		}
		List<FormattedCharSequence> details = details(entry);
		if (!details.isEmpty()) {
			height += 4 + details.size() * Book.smallLine(font);
		}
		return height;
	}

	private int entriesTop(Category category) {
		int width = book.rightPageRight() - book.rightPageLeft();
		int y = book.pagesTop() + Book.PADDING + Book.headingHeight(font, category.root().display().title(), width) + 4;
		return y + rootDescription(category, width).size() * Book.smallLine(font) + 14;
	}

	private List<FormattedCharSequence> rootDescription(Category category, int width) {
		return Book.splitSmall(font, category.root().display().description().copy().withStyle(ChatFormatting.ITALIC), width);
	}

	// --- Drawing -------------------------------------------------------------------------------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		links.clear();
		Book.draw(graphics, book);
		extractCategories(graphics, mouseX, mouseY);
		extractEntries(graphics, mouseX, mouseY);
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	private void extractCategories(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int left = book.leftPageLeft();
		int right = book.leftPageRight();
		int center = (left + right) / 2;
		int y = Book.heading(graphics, font, title, left, right, book.pagesTop() + Book.PADDING) + 2;
		int done = 0;
		int total = 0;
		for (Category category : categories) {
			done += category.done();
			total += category.entries().size();
		}
		Component count = Component.translatable("gui.vanillasugar.advancements.count", done, total);
		Book.small(graphics, font, count, center - Book.smallWidth(font, count) / 2f, y, Book.FADED_INK);
		Book.rule(graphics, left, right, filtersTop() - 8);

		// The filters, side by side in the middle of the page
		Filter[] filters = Filter.values();
		int filtersWidth = -4;
		for (Filter value : filters) {
			filtersWidth += Book.tagWidth(font, filterName(value)) + 4;
		}
		int x = center - filtersWidth / 2;
		for (Filter value : filters) {
			x += Book.tag(graphics, font, links, filterName(value), x, filtersTop(), value == filter, mouseX, mouseY, () -> {
				filter = value;
				lastFilter = value;
				scroll = 0;
			}) + 4;
		}

		int top = categoriesTop();
		int bottom = categoriesBottom();
		categoriesScroll = Mth.clamp(categoriesScroll, 0, Math.max(0, categories.size() * CATEGORY_HEIGHT - (bottom - top)));
		graphics.enableScissor(left - Book.SELECTION_PADDING, top, right + Book.SELECTION_PADDING, bottom);
		int rowTop = top - (int) categoriesScroll;
		for (int i = 0; i < categories.size(); i++) {
			Category category = categories.get(i);
			boolean hovered = mouseX >= left && mouseX < right && mouseY >= Math.max(rowTop, top) && mouseY < Math.min(rowTop + CATEGORY_HEIGHT, bottom);
			if (i == selected || hovered) {
				Book.selection(graphics, left, rowTop, right, rowTop + CATEGORY_HEIGHT - 2, i == selected);
			}
			boolean complete = category.done() == category.entries().size();
			Component progress = complete ? Component.literal("✔")
					: Component.translatable("gui.vanillasugar.advancements.count", category.done(), category.entries().size());
			int progressWidth = Book.smallWidth(font, progress);
			int rowContent = rowTop + (CATEGORY_HEIGHT - 2 - Book.ICON_ROW) / 2;
			Book.small(graphics, font, progress, right - progressWidth, rowContent + (Book.ICON_ROW - Book.smallLine(font)) / 2f + 1,
					complete ? Book.GREEN_INK : Book.FADED_INK);
			int titleWidth = right - progressWidth - 6 - (left + Book.ICON_SIZE + 6);
			Book.iconRow(graphics, font, category.root().display().icon().create(), Book.ellipsize(font, category.root().display().title(), titleWidth),
					left, rowContent, Book.INK);
			rowTop += CATEGORY_HEIGHT;
		}
		graphics.disableScissor();
		Book.scrollbar(graphics, right + Book.SELECTION_PADDING + 3, top, bottom, categoriesScroll, categories.size() * CATEGORY_HEIGHT);
	}

	private static Component filterName(Filter filter) {
		return Component.translatable("gui.vanillasugar.advancements.filter." + filter.name().toLowerCase(Locale.ROOT));
	}

	private void extractEntries(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Category category = category();
		if (category == null) {
			return;
		}
		int left = book.rightPageLeft();
		int right = book.rightPageRight();
		int y = Book.heading(graphics, font, category.root().display().title(), left, right, book.pagesTop() + Book.PADDING) + 4;
		Book.smallLines(graphics, font, rootDescription(category, right - left), left, y, Book.FADED_INK);
		int top = entriesTop(category);
		Book.rule(graphics, left, right, top - 8);
		int bottom = book.pagesBottom() - Book.PADDING / 2;

		List<Entry> entries = shownEntries();
		int contentHeight = contentHeight(entries);
		scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight - (bottom - top)));
		graphics.enableScissor(left - OPEN_PADDING - 1, top, right + OPEN_PADDING + 1, bottom);
		int entryTop = top + OPEN_PADDING - (int) scroll;
		for (Entry entry : entries) {
			int height = entryHeight(entry);
			if (entryTop + height + OPEN_PADDING > top && entryTop - OPEN_PADDING < bottom) {
				boolean hovered = mouseX >= left && mouseX < right && mouseY >= Math.max(top, entryTop) && mouseY < Math.min(bottom, entryTop + height);
				extractEntry(graphics, entry, left, entryTop, right, hovered);
			}
			entryTop += height + ENTRY_GAP;
		}
		Component note = entries.isEmpty() ? Component.translatable("gui.vanillasugar.advancements.empty")
				: !AdvancementCatalog.isComplete() ? Component.translatable("gui.vanillasugar.advancements.more") : null;
		if (note != null) {
			Book.small(graphics, font, note, (left + right) / 2f - Book.smallWidth(font, note) / 2f, entryTop, Book.FADED_INK);
		}
		graphics.disableScissor();
		Book.scrollbar(graphics, right + OPEN_PADDING + 3, top, bottom, scroll, contentHeight);
	}

	private int contentHeight(List<Entry> entries) {
		int height = OPEN_PADDING;
		for (Entry entry : entries) {
			height += entryHeight(entry) + ENTRY_GAP;
		}
		return height + Book.smallLine(font);
	}

	private void extractEntry(GuiGraphicsExtractor graphics, Entry entry, int left, int top, int right, boolean hovered) {
		boolean open = entry.id().equals(expanded) && hasDetails(entry);
		if (open || hovered && hasDetails(entry)) {
			int bottom = top + entryHeight(entry) + OPEN_PADDING;
			Book.selection(graphics, left, top - OPEN_PADDING, right, bottom, open);
		}

		// On the right: made
		int statusWidth = 0;
		if (entry.done()) {
			Component made = Component.literal("✔ ").append(Component.translatable("gui.vanillasugar.advancements.made"));
			statusWidth = Book.smallWidth(font, made);
			Book.small(graphics, font, made, right - statusWidth, top + (Book.ICON_ROW - Book.smallLine(font)) / 2f + 1, Book.GREEN_INK);
		}

		int textLeft = left + Book.ICON_SIZE + 6;
		Component title = entry.secret() ? Component.translatable("gui.vanillasugar.advancements.secret") : entry.display().title();
		FormattedCharSequence shownTitle = Book.ellipsize(font, title, right - statusWidth - 6 - textLeft);
		if (entry.secret()) {
			graphics.text(font, "?", left + 5, top + (Book.ICON_ROW - font.lineHeight) / 2 + 1, Book.FADED_INK, false);
			graphics.text(font, shownTitle, textLeft, top + (Book.ICON_ROW - font.lineHeight) / 2 + 1, Book.FADED_INK, false);
		} else {
			Book.iconRow(graphics, font, entry.display().icon().create(), shownTitle, left, top, QuestJournalScreen.typeColor(entry));
		}
		int y = Book.smallLines(graphics, font, description(entry), textLeft, top + Book.ICON_ROW + 2, Book.FADED_INK);
		if (hasDetails(entry)) {
			Component count = Component.translatable("gui.vanillasugar.advancements.count", entry.requirementsMet(), entry.requirements());
			int countWidth = Book.smallWidth(font, count);
			Book.bar(graphics, textLeft, y + 3, right - countWidth - 4, entry.requirementsMet(), entry.requirements());
			Book.small(graphics, font, count, right - countWidth, y + 1, Book.FADED_INK);
			y += 3 + Book.smallLine(font);
		}
		List<FormattedCharSequence> details = details(entry);
		if (!details.isEmpty()) {
			Book.smallLines(graphics, font, details, textLeft, y + 4, Book.INK);
		}
	}

	// --- Input ---------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick) || links.click(event.x(), event.y())) {
			return true;
		}
		double x = event.x();
		double y = event.y();
		if (x >= book.leftPageLeft() && x < book.leftPageRight() && y >= categoriesTop() && y < categoriesBottom()) {
			int index = (int) ((y - categoriesTop() + categoriesScroll) / CATEGORY_HEIGHT);
			if (index < categories.size()) {
				selected = index;
				lastCategory = categories.get(index).root().id();
				scroll = 0;
				expanded = null;
				return true;
			}
		}
		Category category = category();
		if (category != null && x >= book.rightPageLeft() && x < book.rightPageRight() && y >= entriesTop(category)
				&& y < book.pagesBottom() - Book.PADDING / 2) {
			int entryTop = entriesTop(category) + OPEN_PADDING - (int) scroll;
			for (Entry entry : shownEntries()) {
				int height = entryHeight(entry);
				if (y >= entryTop && y < entryTop + height && hasDetails(entry)) {
					expanded = entry.id().equals(expanded) ? null : entry.id();
					return true;
				}
				entryTop += height + ENTRY_GAP;
			}
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// The advancements key closes them, as the inventory's does every screen; the journal's goes straight to it
		if (minecraft.options.keyAdvancements.matches(event) || minecraft.options.keyInventory.matches(event)) {
			onClose();
			return true;
		}
		if (VanillaSugarClient.OPEN_JOURNAL.matches(event)) {
			minecraft.gui.setScreen(new QuestJournalScreen());
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (scrollY == 0) {
			return false;
		}
		if (x < book.spineX()) {
			categoriesScroll -= scrollY * CATEGORY_HEIGHT;
		} else {
			scroll -= scrollY * SCROLL_STEP;
		}
		return true;
	}
}
