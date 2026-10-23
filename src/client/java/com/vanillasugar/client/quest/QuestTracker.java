package com.vanillasugar.client.quest;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import com.vanillasugar.client.map.FramedPanel;
import com.vanillasugar.client.map.Minimap;
import com.vanillasugar.client.map.SmallText;
import com.vanillasugar.client.quest.AdvancementCatalog.Entry;
import com.vanillasugar.client.quest.QuestLog.Quest;

/**
 * The quest followed, in the top right corner under the minimap, as wide as it, or in its place
 * without one: a "Current quest" tag on its top edge, its chapter, its title and what to do, with how far along it is when it asks for
 * several things. The status effects go below it. Hidden while an advancement is celebrated, so the
 * next quest shows once the banner is gone.
 */
public final class QuestTracker {
	private static final int MARGIN = 4;
	private static final int GAP = 2;
	private static final int BORDER = FramedPanel.BORDER;
	private static final int PADDING = 3;
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

	/** The panel laid out: its lines and where it sits. */
	private record Panel(Quest quest, List<FormattedCharSequence> title, List<FormattedCharSequence> hint, boolean bar, int titleHeight,
			int top, int height) {
	}

	private QuestTracker() {
	}

	private static int width() {
		return Minimap.frameWidth();
	}

	private static @Nullable Panel layout(Minecraft minecraft) {
		if (!QuestLog.isEnabled() || minecraft.player == null || minecraft.getDebugOverlay().showDebugScreen() || AchievementBanner.isShowing()) {
			return null;
		}
		Quest quest = QuestLog.journal(minecraft).current();
		if (quest == null) {
			return null;
		}
		Font font = minecraft.font;
		int contentWidth = width() - 2 * (BORDER + PADDING);
		List<FormattedCharSequence> title = font.split(quest.title(), contentWidth - ICON_SIZE - 4);
		title = title.subList(0, Math.min(title.size(), MAX_TITLE_LINES));
		List<FormattedCharSequence> hint = font.split(quest.hint(), (int) (contentWidth / SmallText.scale()));
		hint = hint.subList(0, Math.min(hint.size(), MAX_HINT_LINES));
		int smallLine = SmallText.lineHeight(font);
		Entry entry = quest.entry();
		boolean bar = entry != null && entry.requirements() > 1;
		int titleHeight = Math.max(ICON_SIZE, title.size() * font.lineHeight);
		int contentHeight = smallLine + 2 + titleHeight + 3 + hint.size() * smallLine + (bar ? 3 + BAR_HEIGHT + 1 + smallLine : 0);
		int top = Minimap.isShown(minecraft) ? Minimap.height(minecraft) : MARGIN;
		return new Panel(quest, title, hint, bar, titleHeight, top, tagOverhang(font) + contentHeight + 2 * (BORDER + PADDING));
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

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		draw(graphics);
	}

	/** Draws the panel, in the HUD or above a screen that frees the cursor. */
	public static void draw(GuiGraphicsExtractor graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		Panel panel = layout(minecraft);
		if (panel == null) {
			return;
		}
		Font font = minecraft.font;
		Quest quest = panel.quest;
		int width = width();
		int left = graphics.guiWidth() - MARGIN - width;
		int frameTop = panel.top + tagOverhang(font);
		FramedPanel.draw(graphics, left, frameTop, width, panel.height - tagOverhang(font));
		Component tag = Component.translatable("gui.vanillasugar.journal.current");
		int tagWidth = SmallText.width(font, tag) + 6;
		int tagLeft = left + TAG_LEFT;
		graphics.fill(tagLeft, panel.top, tagLeft + tagWidth, panel.top + tagHeight(font), OUTLINE);
		graphics.fill(tagLeft + 1, panel.top + 1, tagLeft + tagWidth - 1, panel.top + tagHeight(font) - 1, TAG);
		drawSmall(graphics, font, tag.getVisualOrderText(), tagLeft + 3, panel.top + 2, TAG_TEXT, false);

		int contentWidth = width - 2 * (BORDER + PADDING);
		int smallLine = SmallText.lineHeight(font);
		int x = left + BORDER + PADDING;
		int y = frameTop + Math.max(BORDER + PADDING, tagHeight(font) - tagOverhang(font) + 2);
		SmallText.left(graphics, font, quest.chapter().title(), x, y, GOLD);
		y += smallLine + 2;
		graphics.fakeItem(quest.icon(), x, y + (panel.titleHeight - ICON_SIZE) / 2);
		int titleTop = y + (panel.titleHeight - panel.title.size() * font.lineHeight) / 2;
		for (FormattedCharSequence line : panel.title) {
			graphics.text(font, line, x + ICON_SIZE + 4, titleTop, TITLE, true);
			titleTop += font.lineHeight;
		}
		y += panel.titleHeight + 3;
		for (FormattedCharSequence line : panel.hint) {
			drawSmall(graphics, font, line, x, y, HINT, true);
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
