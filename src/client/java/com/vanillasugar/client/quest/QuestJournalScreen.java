package com.vanillasugar.client.quest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import com.vanillasugar.client.LabeledKeys;
import com.vanillasugar.client.VanillaSugarClient;
import com.vanillasugar.client.quest.AdvancementCatalog.Entry;
import com.vanillasugar.client.quest.QuestLog.ChapterState;
import com.vanillasugar.client.quest.QuestLog.Journal;
import com.vanillasugar.client.quest.QuestLog.Quest;
import com.vanillasugar.client.quest.QuestLog.Status;

/**
 * The quest journal, an open book (see Book). The left page lists the chapters with how far along
 * each one is; the right page, the quests of the chapter chosen: those done, those open with what to
 * do and those still locked, greyed out with the advancement they come after, then apart those done.
 * Any open quest can be followed, shown on the side of the screen in game and set apart on the page.
 */
public class QuestJournalScreen extends Screen {
	private static final int CHAPTER_HEIGHT = 22;
	private static final int QUEST_GAP = 12;
	/** Around the quest followed, inside its background. */
	private static final int FOLLOWED_PADDING = Book.SELECTION_PADDING;
	private static final int SCROLL_STEP = 24;

	private Book.Layout book = Book.Layout.centered(0, 0);
	private Journal journal = new Journal(List.of(), null);
	private int selected = -1;
	private double chaptersScroll;
	private double questsScroll;
	private final Book.Links links = new Book.Links();

	public QuestJournalScreen() {
		super(Component.translatable("gui.vanillasugar.journal"));
	}

	@Override
	protected void init() {
		book = Book.Layout.centered(width, height);
		journal = QuestLog.journal(minecraft);
		if (selected < 0) {
			selected = 0;
			Quest current = journal.current();
			for (int i = 0; i < journal.chapters().size(); i++) {
				if (current != null && journal.chapters().get(i).chapter() == current.chapter()) {
					selected = i;
				}
			}
		}
	}

	@Override
	public void tick() {
		journal = QuestLog.journal(minecraft);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// --- Layout --------------------------------------------------------------------------------

	private int chaptersTop() {
		return book.pagesTop() + Book.PADDING + Book.headingHeight(font, title, book.leftPageRight() - book.leftPageLeft()) + 2
				+ Book.smallLine(font) + 14;
	}

	private int footerTop() {
		return book.pagesBottom() - Book.PADDING - Book.smallLine(font) - 4 - Book.tagHeight(font);
	}

	private int chaptersBottom() {
		return footerTop() - 12;
	}

	private @Nullable ChapterState chapter() {
		return selected >= 0 && selected < journal.chapters().size() ? journal.chapters().get(selected) : null;
	}

	// --- Drawing -------------------------------------------------------------------------------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		links.clear();
		Book.draw(graphics, book);
		extractChapters(graphics, mouseX, mouseY);
		extractQuests(graphics, mouseX, mouseY);
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	private void extractChapters(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int left = book.leftPageLeft();
		int right = book.leftPageRight();
		int center = (left + right) / 2;
		int y = Book.heading(graphics, font, title, left, right, book.pagesTop() + Book.PADDING) + 2;
		Component count = Component.translatable("gui.vanillasugar.journal.count", journal.done(), journal.total());
		Book.small(graphics, font, count, center - Book.smallWidth(font, count) / 2f, y, Book.FADED_INK);
		int top = chaptersTop();
		Book.rule(graphics, left, right, top - 8);

		int bottom = chaptersBottom();
		chaptersScroll = Mth.clamp(chaptersScroll, 0, Math.max(0, journal.chapters().size() * CHAPTER_HEIGHT - (bottom - top)));
		graphics.enableScissor(left - Book.SELECTION_PADDING, top, right + Book.SELECTION_PADDING, bottom);
		int rowTop = top - (int) chaptersScroll;
		for (int i = 0; i < journal.chapters().size(); i++) {
			ChapterState state = journal.chapters().get(i);
			boolean hovered = mouseX >= left && mouseX < right && mouseY >= Math.max(rowTop, top) && mouseY < Math.min(rowTop + CHAPTER_HEIGHT, bottom);
			if (i == selected || hovered) {
				// The ribbon marks the chapter open on the right page
				Book.selection(graphics, left, rowTop, right, rowTop + CHAPTER_HEIGHT - 2, i == selected);
			}
			boolean complete = state.done() == state.quests().size();
			Component progress = complete ? Component.literal("✔")
					: Component.translatable("gui.vanillasugar.advancements.count", state.done(), state.quests().size());
			int progressWidth = Book.smallWidth(font, progress);
			int rowContent = rowTop + (CHAPTER_HEIGHT - 2 - Book.ICON_ROW) / 2;
			Book.small(graphics, font, progress, right - progressWidth, rowContent + (Book.ICON_ROW - Book.smallLine(font)) / 2f + 1,
					complete ? Book.GREEN_INK : Book.FADED_INK);
			int titleWidth = right - progressWidth - 6 - (left + Book.ICON_SIZE + 6);
			Book.iconRow(graphics, font, new ItemStack(state.chapter().icon()), Book.ellipsize(font, state.chapter().title(), titleWidth), left,
					rowContent, state.locked() ? Book.FADED_INK : Book.INK);
			rowTop += CHAPTER_HEIGHT;
		}
		graphics.disableScissor();
		Book.scrollbar(graphics, right + Book.SELECTION_PADDING + 3, top, bottom, chaptersScroll, journal.chapters().size() * CHAPTER_HEIGHT);

		// Following the journal or not, at the bottom of the page
		int footer = footerTop();
		Book.rule(graphics, left, right, footer - 7);
		Component note = QuestLog.isEnabled()
				? Component.translatable("gui.vanillasugar.journal.key", LabeledKeys.printedName(VanillaSugarClient.OPEN_JOURNAL))
				: Component.translatable("gui.vanillasugar.journal.paused");
		Book.small(graphics, font, note, center - Book.smallWidth(font, note) / 2f, footer, Book.FADED_INK);
		boolean enabled = QuestLog.isEnabled();
		Component toggle = Component.translatable(enabled ? "gui.vanillasugar.journal.disable" : "gui.vanillasugar.journal.enable");
		Book.tag(graphics, font, links, toggle, center - Book.tagWidth(font, toggle) / 2, footer + Book.smallLine(font) + 4, false,
				mouseX, mouseY, () -> QuestLog.setEnabled(!enabled));
	}

	private int questsTop(ChapterState state) {
		int width = book.rightPageRight() - book.rightPageLeft();
		int y = book.pagesTop() + Book.PADDING + Book.headingHeight(font, state.chapter().title(), width) + 4;
		return y + description(state, width).size() * Book.smallLine(font) + 14;
	}

	private List<FormattedCharSequence> description(ChapterState state, int width) {
		return Book.splitSmall(font, state.chapter().description().copy().withStyle(ChatFormatting.ITALIC), width);
	}

	private void extractQuests(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		ChapterState state = chapter();
		if (state == null) {
			return;
		}
		int left = book.rightPageLeft();
		int right = book.rightPageRight();
		int y = Book.heading(graphics, font, state.chapter().title(), left, right, book.pagesTop() + Book.PADDING) + 4;
		Book.smallLines(graphics, font, description(state, right - left), left, y, Book.FADED_INK);
		int top = questsTop(state);
		Book.rule(graphics, left, right, top - 8);
		int bottom = book.pagesBottom() - Book.PADDING / 2;

		List<Quest> quests = ordered(state);
		int doneCount = state.done();
		int contentHeight = FOLLOWED_PADDING + (doneCount > 0 ? sectionHeight() : 0);
		for (Quest quest : quests) {
			contentHeight += questHeight(quest) + QUEST_GAP;
		}
		questsScroll = Mth.clamp(questsScroll, 0, Math.max(0, contentHeight - (bottom - top)));
		graphics.enableScissor(left - FOLLOWED_PADDING - 1, top, right + FOLLOWED_PADDING + 1, bottom);
		// Room above the first quest for the background of the followed one
		int questTop = top + FOLLOWED_PADDING - (int) questsScroll;
		int firstLink = links.size();
		List<ScreenRectangle> lockedIcons = new ArrayList<>();
		boolean doneSection = false;
		for (Quest quest : quests) {
			if (quest.status() == Status.DONE && !doneSection) {
				doneSection = true;
				extractSection(graphics, Component.translatable("gui.vanillasugar.journal.done_section", doneCount), left, questTop, right);
				questTop += sectionHeight();
			}
			int height = questHeight(quest);
			if (questTop + height + FOLLOWED_PADDING > top && questTop - FOLLOWED_PADDING < bottom) {
				extractQuest(graphics, quest, left, questTop, right, mouseX, mouseY);
				if (quest.status() == Status.LOCKED) {
					lockedIcons.add(new ScreenRectangle(left, questTop + (Book.ICON_ROW - Book.ICON_SIZE) / 2, Book.ICON_SIZE, Book.ICON_SIZE));
				}
			}
			questTop += height + QUEST_GAP;
		}
		Book.dim(graphics, lockedIcons);
		graphics.disableScissor();
		links.clip(firstLink, top, bottom);
		Book.scrollbar(graphics, right + FOLLOWED_PADDING + 3, top, bottom, questsScroll, contentHeight);
	}

	/** The quests that can be followed first, then those waiting on another one, then those done. */
	private static List<Quest> ordered(ChapterState state) {
		List<Quest> quests = new ArrayList<>(state.quests());
		quests.sort(Comparator.comparingInt(quest -> switch (quest.status()) {
			case OPEN -> 0;
			case LOCKED -> 1;
			case DONE -> 2;
		}));
		return quests;
	}

	private int sectionHeight() {
		return Book.smallLine(font) + 6;
	}

	/** A small heading over the quests done, a line running after it. */
	private void extractSection(GuiGraphicsExtractor graphics, Component text, int left, int top, int right) {
		Book.small(graphics, font, text, left, top, Book.FADED_INK);
		int lineY = top + Book.smallLine(font) / 2 - 1;
		graphics.fill(left + Book.smallWidth(font, text) + 4, lineY, right, lineY + 1, Book.PAGE_EDGE);
	}

	private List<FormattedCharSequence> details(Quest quest) {
		int width = book.rightPageRight() - book.rightPageLeft() - Book.ICON_SIZE - 6;
		return switch (quest.status()) {
			case DONE -> List.of();
			case OPEN -> Book.splitSmall(font, quest.hint(), width);
			case LOCKED -> Book.splitSmall(font, quest.after() != null
					? Component.translatable("gui.vanillasugar.journal.after", quest.after())
					: Component.translatable("gui.vanillasugar.journal.unknown"), width);
		};
	}

	private int questHeight(Quest quest) {
		int height = Book.ICON_ROW;
		List<FormattedCharSequence> details = details(quest);
		if (!details.isEmpty()) {
			height += 2 + details.size() * Book.smallLine(font);
		}
		if (hasBar(quest)) {
			height += 3 + Book.smallLine(font);
		}
		return height;
	}

	private static boolean hasBar(Quest quest) {
		return quest.status() == Status.OPEN && quest.entry() != null && quest.entry().requirements() > 1;
	}

	private boolean isFollowed(Quest quest) {
		Quest current = journal.current();
		return QuestLog.isEnabled() && current != null && current.id().equals(quest.id());
	}

	private void extractQuest(GuiGraphicsExtractor graphics, Quest quest, int left, int top, int right, int mouseX, int mouseY) {
		boolean followed = isFollowed(quest);
		if (followed) {
			int bottom = top + questHeight(quest) + FOLLOWED_PADDING;
			Book.selection(graphics, left, top - FOLLOWED_PADDING, right, bottom, true);
		}

		// On the right: done, followed, or a tag to follow it
		int tagTop = top + (Book.ICON_ROW - Book.tagHeight(font)) / 2;
		int statusWidth = 0;
		if (quest.status() == Status.DONE) {
			Component done = Component.literal("✔");
			statusWidth = Book.smallWidth(font, done);
			Book.small(graphics, font, done, right - statusWidth, top + (Book.ICON_ROW - Book.smallLine(font)) / 2f + 1, Book.GREEN_INK);
		} else if (followed) {
			Component label = Component.translatable("gui.vanillasugar.journal.followed");
			statusWidth = Book.tagWidth(font, label);
			Book.tag(graphics, font, links, label, right - statusWidth, tagTop, true, mouseX, mouseY, null);
		} else if (quest.status() == Status.OPEN && QuestLog.isEnabled()) {
			Component label = Component.translatable("gui.vanillasugar.journal.follow");
			statusWidth = Book.tagWidth(font, label);
			Book.tag(graphics, font, links, label, right - statusWidth, tagTop, false, mouseX, mouseY, () -> QuestLog.follow(quest.id()));
		}

		int titleColor = switch (quest.status()) {
			case DONE -> Book.GREEN_INK;
			case LOCKED -> Book.LOCKED_INK;
			case OPEN -> typeColor(quest.entry());
		};
		int textLeft = left + Book.ICON_SIZE + 6;
		Book.iconRow(graphics, font, quest.icon(), Book.ellipsize(font, quest.title(), right - statusWidth - 6 - textLeft), left, top, titleColor);
		int y = top + Book.ICON_ROW + 2;
		y = Book.smallLines(graphics, font, details(quest), textLeft, y, quest.status() == Status.LOCKED ? Book.LOCKED_INK : Book.INK);
		if (hasBar(quest)) {
			Entry entry = quest.entry();
			Component count = Component.translatable("gui.vanillasugar.advancements.count", entry.requirementsMet(), entry.requirements());
			int countWidth = Book.smallWidth(font, count);
			Book.bar(graphics, textLeft, y + 3, right - countWidth - 4, entry.requirementsMet(), entry.requirements());
			Book.small(graphics, font, count, right - countWidth, y + 1, Book.FADED_INK);
		}
	}

	static int typeColor(@Nullable Entry entry) {
		if (entry == null) {
			return Book.INK;
		}
		AdvancementType type = entry.display().type();
		return switch (type) {
			case TASK -> Book.INK;
			case GOAL -> Book.GOLD_INK;
			case CHALLENGE -> Book.PURPLE_INK;
		};
	}

	// --- Input ---------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (links.click(event.x(), event.y())) {
			journal = QuestLog.journal(minecraft);
			return true;
		}
		if (event.x() >= book.leftPageLeft() && event.x() < book.leftPageRight() && event.y() >= chaptersTop() && event.y() < chaptersBottom()) {
			int index = (int) ((event.y() - chaptersTop() + chaptersScroll) / CHAPTER_HEIGHT);
			if (index < journal.chapters().size()) {
				selected = index;
				questsScroll = 0;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (scrollY == 0) {
			return false;
		}
		if (x < book.spineX()) {
			chaptersScroll -= scrollY * CHAPTER_HEIGHT;
		} else {
			questsScroll -= scrollY * SCROLL_STEP;
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Its key closes it, like the inventory's, which closes every screen
		if (VanillaSugarClient.OPEN_JOURNAL.matches(event) || minecraft.options.keyInventory.matches(event)) {
			onClose();
			return true;
		}
		// The advancements key goes straight to them
		if (minecraft.options.keyAdvancements.matches(event)) {
			QuestButtons.openAdvancements(minecraft);
			return true;
		}
		return super.keyPressed(event);
	}
}
