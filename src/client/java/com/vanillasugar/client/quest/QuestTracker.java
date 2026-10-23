package com.vanillasugar.client.quest;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import com.vanillasugar.client.ClientSettings;
import com.vanillasugar.client.ClientSettings.QuestTrackerDisplay;
import com.vanillasugar.client.map.FramedPanel;
import com.vanillasugar.client.map.Minimap;
import com.vanillasugar.client.map.SmallText;
import com.vanillasugar.client.quest.AdvancementCatalog.Entry;
import com.vanillasugar.client.quest.QuestLog.Journal;
import com.vanillasugar.client.quest.QuestLog.Quest;
import com.vanillasugar.client.quest.QuestLog.Status;

/**
 * The quest followed, in the top right corner under the minimap, as wide as it, or in its place
 * without one: a "Current quest" tag on its top edge, its chapter, its title and what to do, with how far along it is when it asks for
 * several things. The status effects go below it. A key cycles it through full, compact (its title
 * alone) and hidden.
 *
 * <p>When the quest is done, it stays a few seconds: its tag turns green, its title is struck through
 * and ticked, then it slides off to the right and the next one slides in.
 */
public final class QuestTracker {
	private static final int MARGIN = 4;
	/** Between the minimap's coordinates and the tag on the panel's top edge. */
	private static final int MINIMAP_GAP = 16;
	private static final int GAP = 2;
	private static final int BORDER = FramedPanel.BORDER;
	private static final int PADDING = 6;
	private static final int ICON_SIZE = 16;
	private static final int MAX_TITLE_LINES = 2;
	private static final int MAX_HINT_LINES = 6;
	private static final int BAR_HEIGHT = 3;
	// The theme's colors (tools/gui_theme.py)
	private static final int GOLD = 0xFFF6DA8A;
	private static final int TITLE = 0xFFFFFFFF;
	private static final int HINT = 0xFFD2BBB5;
	private static final int MUTED = 0xFFA38883;
	private static final int BAR_BACKGROUND = 0xFF1B0F0F;
	private static final int BAR_FILL = 0xFF80A642;
	private static final int TAG = 0xFFC8963C;
	private static final int TAG_TEXT = 0xFF2B1F1F;
	private static final int OUTLINE = 0xFF130809;
	private static final int TAG_LEFT = 6;
	private static final int DONE_TAG = 0xFF80A642;
	private static final int DONE_TITLE = 0xFFA6C85C;
	private static final int STRIKE = 0xFFECD6D0;
	/** Like the buttons in the bottom right corner. */
	private static final int BACKGROUND_HOVERED = 0xFF473130;
	/** How long a quest done stays, its title struck through over the first moments. */
	private static final long DONE_MILLIS = 3500;
	private static final long STRIKE_MILLIS = 700;
	/** How long the panel takes to slide out, and the next one in. */
	private static final long SLIDE_MILLIS = 300;

	/** The quest last shown, to notice when it is done. */
	private static @Nullable Quest shown;
	/** The quest just done, while it stays. */
	private static @Nullable Quest done;
	private static long doneAt;
	private static long nextAt;

	/** The panel laid out: its lines and where it sits. */
	private record Panel(Quest quest, long doneFor, boolean full, List<FormattedCharSequence> title, List<FormattedCharSequence> hint, boolean bar,
			int titleHeight, int top, int height) {
	}

	private QuestTracker() {
	}

	/** Forgets the quest shown, leaving a world. */
	public static void reset() {
		shown = null;
		done = null;
		nextAt = 0;
	}

	/** The quest to show: the one just done while it stays, else the current one. */
	private static @Nullable Quest displayed(Journal journal, long now) {
		if (done != null && now - doneAt < DONE_MILLIS) {
			return done;
		}
		if (done != null) {
			done = null;
			nextAt = now;
		}
		Quest current = journal.current();
		if (shown != null && (current == null || !current.id().equals(shown.id()))) {
			Quest previous = journal.find(shown.id());
			if (previous != null && previous.status() == Status.DONE) {
				done = previous;
				doneAt = now;
				shown = current;
				return done;
			}
		}
		shown = current;
		return current;
	}

	private static int width() {
		return Minimap.frameWidth();
	}

	private static @Nullable Panel layout(Minecraft minecraft) {
		if (!QuestLog.isEnabled() || minecraft.player == null) {
			return null;
		}
		long now = Util.getMillis();
		Quest quest = displayed(QuestLog.journal(minecraft), now);
		QuestTrackerDisplay display = ClientSettings.questTrackerDisplay();
		if (quest == null || display == QuestTrackerDisplay.HIDDEN || minecraft.getDebugOverlay().showDebugScreen()) {
			return null;
		}
		long doneFor = quest == done ? now - doneAt : -1;
		Font font = minecraft.font;
		int contentWidth = width() - 2 * (BORDER + PADDING);
		List<FormattedCharSequence> title = font.split(quest.title(), contentWidth - ICON_SIZE - 4);
		title = title.subList(0, Math.min(title.size(), MAX_TITLE_LINES));
		boolean full = display == QuestTrackerDisplay.FULL;
		List<FormattedCharSequence> hint = full ? font.split(quest.hint(), (int) (contentWidth / SmallText.scale())) : List.of();
		hint = hint.subList(0, Math.min(hint.size(), MAX_HINT_LINES));
		int smallLine = SmallText.lineHeight(font);
		Entry entry = quest.entry();
		boolean bar = full && entry != null && entry.requirements() > 1;
		int titleHeight = Math.max(ICON_SIZE, title.size() * font.lineHeight);
		int contentHeight = full ? smallLine + 2 + titleHeight + 3 + hint.size() * smallLine + (bar ? 3 + BAR_HEIGHT + 1 + smallLine : 0) : titleHeight;
		int top = Minimap.isShown(minecraft) ? Minimap.height(minecraft) + MINIMAP_GAP : MARGIN;
		return new Panel(quest, doneFor, full, title, hint, bar, titleHeight, top, tagOverhang(font) + contentHeight + 2 * (BORDER + PADDING));
	}

	private static int tagHeight(Font font) {
		return SmallText.lineHeight(font) + 3;
	}

	/** How far the tag, astride the frame's top edge, rises above it; the frame starts that much lower. */
	private static int tagOverhang(Font font) {
		return tagHeight(font) / 2;
	}

	/** Everything the minimap and the panel take from the top of the screen, for the status effects below. */
	public static int bottom(Minecraft minecraft) {
		Panel panel = layout(minecraft);
		if (panel != null) {
			return panel.top + panel.height + GAP;
		}
		return Minimap.isShown(minecraft) ? Minimap.height(minecraft) : 0;
	}

	/** Shows the next of full, compact and hidden, and says which. */
	public static void cycle(Minecraft minecraft) {
		ClientSettings.setQuestTrackerDisplay(ClientSettings.questTrackerDisplay().next());
		Component display = Component.translatable("gui.vanillasugar.quest_tracker." + ClientSettings.questTrackerDisplay().name().toLowerCase(Locale.ROOT));
		minecraft.gui.hud.setOverlayMessage(Component.translatable("gui.vanillasugar.quest_tracker", display), false);
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		draw(graphics, -1, -1);
	}

	/**
	 * The quest shown under the cursor, its tag included, where a screen frees the cursor: a click there
	 * opens it in the journal.
	 */
	public static @Nullable Quest questAt(Minecraft minecraft, int guiWidth, double mouseX, double mouseY) {
		Panel panel = layout(minecraft);
		if (panel == null) {
			return null;
		}
		int left = guiWidth - MARGIN - width();
		return mouseX >= left && mouseX < left + width() && mouseY >= panel.top && mouseY < panel.top + panel.height ? panel.quest : null;
	}

	/** Draws the panel, in the HUD or above a screen that frees the cursor, lit when hovered there. */
	public static void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Minecraft minecraft = Minecraft.getInstance();
		Panel panel = layout(minecraft);
		if (panel == null) {
			return;
		}
		boolean hovered = questAt(minecraft, graphics.guiWidth(), mouseX, mouseY) != null;
		Font font = minecraft.font;
		Quest quest = panel.quest;
		boolean questDone = panel.doneFor >= 0;
		int width = width();
		int left = graphics.guiWidth() - MARGIN - width;
		graphics.pose().pushMatrix();
		graphics.pose().translate(slide(panel) * (width + MARGIN), 0);
		int frameTop = panel.top + tagOverhang(font);
		FramedPanel.draw(graphics, left, frameTop, width, panel.height - tagOverhang(font));
		if (hovered) {
			graphics.fill(left + BORDER, frameTop + BORDER, left + width - BORDER, panel.top + panel.height - BORDER, BACKGROUND_HOVERED);
		}
		Component tag = Component.translatable(questDone ? "gui.vanillasugar.journal.done" : "gui.vanillasugar.journal.current");
		int tagWidth = SmallText.width(font, tag) + 6;
		int tagLeft = left + TAG_LEFT;
		graphics.fill(tagLeft, panel.top, tagLeft + tagWidth, panel.top + tagHeight(font), OUTLINE);
		graphics.fill(tagLeft + 1, panel.top + 1, tagLeft + tagWidth - 1, panel.top + tagHeight(font) - 1, questDone ? DONE_TAG : TAG);
		drawSmall(graphics, font, tag.getVisualOrderText(), tagLeft + 3, panel.top + 2, TAG_TEXT, false);

		int contentWidth = width - 2 * (BORDER + PADDING);
		int smallLine = SmallText.lineHeight(font);
		int x = left + BORDER + PADDING;
		int y = frameTop + Math.max(BORDER + PADDING, tagHeight(font) - tagOverhang(font) + 2);
		if (panel.full) {
			SmallText.left(graphics, font, quest.chapter().title(), x, y, GOLD);
			y += smallLine + 2;
		}
		int iconTop = y + (panel.titleHeight - ICON_SIZE) / 2;
		graphics.fakeItem(quest.icon(), x, iconTop);
		if (questDone) {
			// A tick over the icon's corner, above the item
			graphics.nextStratum();
			graphics.text(font, "✔", x + ICON_SIZE - 5, iconTop + ICON_SIZE - 7, DONE_TITLE, true);
		}
		float struck = questDone ? Mth.clamp(panel.doneFor / (float) STRIKE_MILLIS, 0, 1) : 0;
		int titleTop = y + (panel.titleHeight - panel.title.size() * font.lineHeight) / 2;
		for (FormattedCharSequence line : panel.title) {
			int titleLeft = x + ICON_SIZE + 4;
			graphics.text(font, line, titleLeft, titleTop, questDone ? DONE_TITLE : TITLE, true);
			if (struck > 0) {
				int strikeRight = titleLeft + Math.round((font.width(line) + 1) * easeOut(struck));
				graphics.fill(titleLeft - 1, titleTop + 3, strikeRight, titleTop + 4, STRIKE);
				graphics.fill(titleLeft, titleTop + 4, strikeRight + 1, titleTop + 5, OUTLINE);
			}
			titleTop += font.lineHeight;
		}
		y += panel.titleHeight + 3;
		for (FormattedCharSequence line : panel.hint) {
			drawSmall(graphics, font, line, x, y, questDone ? MUTED : HINT, true);
			y += smallLine;
		}
		if (panel.bar) {
			Entry entry = quest.entry();
			y += 3;
			graphics.fill(x, y, x + contentWidth, y + BAR_HEIGHT, BAR_BACKGROUND);
			graphics.fill(x, y, x + contentWidth * entry.requirementsMet() / entry.requirements(), y + BAR_HEIGHT, BAR_FILL);
			y += BAR_HEIGHT + 1;
			SmallText.left(graphics, font, Component.translatable("gui.vanillasugar.advancements.count", entry.requirementsMet(), entry.requirements()),
					x, y, MUTED);
		}
		graphics.pose().popMatrix();
	}

	/** How far off to the right the panel is, in widths: sliding out once done, the next one sliding in. */
	private static float slide(Panel panel) {
		if (panel.doneFor >= 0) {
			return easeIn(Mth.clamp((panel.doneFor - (DONE_MILLIS - SLIDE_MILLIS)) / (float) SLIDE_MILLIS, 0, 1));
		}
		return 1 - easeOut(Mth.clamp((Util.getMillis() - nextAt) / (float) SLIDE_MILLIS, 0, 1));
	}

	private static float easeOut(float t) {
		return 1 - (1 - t) * (1 - t);
	}

	private static float easeIn(float t) {
		return t * t;
	}

	private static void drawSmall(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence line, float x, float y, int color, boolean shadow) {
		float scale = SmallText.scale();
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.text(font, line, 0, 0, color, shadow);
		graphics.pose().popMatrix();
	}
}
