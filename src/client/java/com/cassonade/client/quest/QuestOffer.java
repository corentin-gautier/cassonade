package com.cassonade.client.quest;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import com.cassonade.client.CassonadeClient;
import com.cassonade.client.LabeledKeys;
import com.cassonade.client.map.FramedPanel;
import com.cassonade.client.map.Minimap;
import com.cassonade.client.map.SmallText;

/**
 * Offers the quest journal without stopping the game: a small panel where the quest followed would
 * sit, under the minimap. The cursor is not free during play, so each button shows a key that answers
 * it (the journal's to follow, the tracker's to decline); the buttons can also be clicked from the
 * chat or the inventory. Left unanswered, it slides away and asks again the next time the player joins
 * the world. Its time only runs while nothing covers it, so a player answering with the mouse has time.
 */
public final class QuestOffer {
	private static final int DURATION_TICKS = 20 * 20;
	private static final long SLIDE_MILLIS = 300;
	private static final int MARGIN = 4;
	private static final int BORDER = FramedPanel.BORDER;
	private static final int PADDING = 6;
	private static final int BUTTON_HEIGHT = 15;
	private static final int BUTTON_GAP = 2;
	private static final int KEY_MARGIN = 2;
	private static final int KEY_SIZE = BUTTON_HEIGHT - 2 * KEY_MARGIN;
	/** The font's capitals, and the blank column that closes each glyph's width. */
	private static final int GLYPH_HEIGHT = 7;
	private static final int GLYPH_SPACING = 1;
	private static final int BAR_HEIGHT = 2;
	private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
	private static final Identifier BUTTON_HOVERED = Identifier.withDefaultNamespace("widget/button_highlighted");
	// The theme's colors (tools/gui_theme.py)
	private static final int GOLD = 0xFFF6DA8A;
	private static final int TEXT = 0xFFD2BBB5;
	private static final int MUTED = 0xFFA38883;
	private static final int LABEL = 0xFFFFFFFF;
	private static final int KEY = 0xFFC8963C;
	private static final int KEY_TEXT = 0xFF2B1F1F;
	private static final int OUTLINE = 0xFF130809;
	private static final int BAR_BACKGROUND = 0xFF1B0F0F;
	private static final int BAR_FILL = 0xFFC8963C;

	/** Whether it was already offered since joining the world. */
	private static boolean offered;
	private static boolean showing;
	private static int ticksLeft;
	private static long shownAt;
	/** When it started sliding away, -1 until then. */
	private static long leavingAt = -1;

	private record Panel(List<FormattedCharSequence> text, List<FormattedCharSequence> note, int left, int top, int height, int buttonsTop) {
	}

	private QuestOffer() {
	}

	/** Forgets the offer, leaving a world. */
	public static void reset() {
		offered = false;
		showing = false;
		leavingAt = -1;
	}

	public static void tick(Minecraft minecraft) {
		if (!showing) {
			if (!offered && QuestLog.needsChoice() && minecraft.player != null && minecraft.gui.screen() == null && uncovered(minecraft)) {
				offered = true;
				showing = true;
				ticksLeft = DURATION_TICKS;
				shownAt = Util.getMillis();
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.0F, 1.0F));
			}
			return;
		}
		if (leavingAt >= 0) {
			if (Util.getMillis() - leavingAt >= SLIDE_MILLIS) {
				showing = false;
				leavingAt = -1;
			}
			return;
		}
		// Answered elsewhere, in the journal
		if (!QuestLog.needsChoice()) {
			showing = false;
			return;
		}
		if (minecraft.gui.screen() == null && uncovered(minecraft) && --ticksLeft <= 0) {
			leavingAt = Util.getMillis();
		}
	}

	/** The same conditions as the quest tracker, in whose place it sits. */
	private static boolean uncovered(Minecraft minecraft) {
		return !minecraft.gui.hud.isHidden() && !minecraft.getDebugOverlay().showDebugScreen();
	}

	/** Whether it can be answered now. */
	public static boolean isShown() {
		return showing && leavingAt < 0 && QuestLog.needsChoice();
	}

	/** Answers the offer if it is on the screen; if not, the key that called this keeps its usual role. */
	public static boolean answer(Minecraft minecraft, boolean follow) {
		if (!isShown() || !uncovered(minecraft)) {
			return false;
		}
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		QuestLog.setEnabled(follow);
		if (follow) {
			// The quest followed takes its place
			showing = false;
			QuestTracker.slideIn();
		} else {
			leavingAt = Util.getMillis();
		}
		return true;
	}

	private static Panel layout(Minecraft minecraft, int guiWidth) {
		Font font = minecraft.font;
		int width = Minimap.frameWidth();
		int textWidth = (int) ((width - 2 * (BORDER + PADDING)) / SmallText.scale());
		List<FormattedCharSequence> text = font.split(Component.translatable("gui.cassonade.quest_opt_in.text"), textWidth);
		List<FormattedCharSequence> note = font.split(Component.translatable("gui.cassonade.quest_opt_in.note"), textWidth);
		int smallLine = SmallText.lineHeight(font);
		int buttonsOffset = BORDER + PADDING + smallLine + 2 + text.size() * smallLine + 3 + note.size() * smallLine + 4;
		int height = buttonsOffset + 2 * BUTTON_HEIGHT + BUTTON_GAP + 3 + BAR_HEIGHT + PADDING + BORDER;
		int top = QuestTracker.top(minecraft);
		return new Panel(text, note, guiWidth - MARGIN - width, top, height, top + buttonsOffset);
	}

	/** Where the panel ends, for what goes below it. */
	public static int bottom(Minecraft minecraft, int guiWidth) {
		Panel panel = layout(minecraft, guiWidth);
		return panel.top + panel.height;
	}

	/** The answer of the button under the cursor, where a screen frees it. */
	public static @Nullable Boolean choiceAt(Minecraft minecraft, int guiWidth, double mouseX, double mouseY) {
		if (!isShown() || !uncovered(minecraft)) {
			return null;
		}
		Panel panel = layout(minecraft, guiWidth);
		int buttonLeft = panel.left + BORDER + PADDING;
		int buttonWidth = Minimap.frameWidth() - 2 * (BORDER + PADDING);
		if (mouseX < buttonLeft || mouseX >= buttonLeft + buttonWidth) {
			return null;
		}
		if (mouseY >= panel.buttonsTop && mouseY < panel.buttonsTop + BUTTON_HEIGHT) {
			return true;
		}
		int second = panel.buttonsTop + BUTTON_HEIGHT + BUTTON_GAP;
		return mouseY >= second && mouseY < second + BUTTON_HEIGHT ? false : null;
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		draw(graphics, -1, -1);
	}

	/** Draws the panel, in the HUD or above a screen that frees the cursor, its buttons lit when hovered there. */
	public static void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!showing || leavingAt < 0 && !QuestLog.needsChoice() || !uncovered(minecraft)) {
			return;
		}
		Font font = minecraft.font;
		Panel panel = layout(minecraft, graphics.guiWidth());
		int width = Minimap.frameWidth();
		Boolean hovered = choiceAt(minecraft, graphics.guiWidth(), mouseX, mouseY);
		graphics.pose().pushMatrix();
		graphics.pose().translate(slide() * (width + MARGIN), 0);
		FramedPanel.draw(graphics, panel.left, panel.top, width, panel.height);
		int x = panel.left + BORDER + PADDING;
		int contentWidth = width - 2 * (BORDER + PADDING);
		int smallLine = SmallText.lineHeight(font);
		int y = panel.top + BORDER + PADDING;
		SmallText.left(graphics, font, Component.translatable("gui.cassonade.quest_opt_in.title"), x, y, GOLD);
		y += smallLine + 2;
		for (FormattedCharSequence line : panel.text) {
			SmallText.left(graphics, font, line, x, y, TEXT);
			y += smallLine;
		}
		y += 3;
		for (FormattedCharSequence line : panel.note) {
			SmallText.left(graphics, font, line, x, y, MUTED);
			y += smallLine;
		}
		drawButton(graphics, font, CassonadeClient.OPEN_JOURNAL, Component.translatable("gui.cassonade.quest_opt_in.yes"), x, panel.buttonsTop, contentWidth,
				Boolean.TRUE.equals(hovered));
		int second = panel.buttonsTop + BUTTON_HEIGHT + BUTTON_GAP;
		drawButton(graphics, font, CassonadeClient.QUEST_TRACKER, Component.translatable("gui.cassonade.quest_opt_in.no"), x, second, contentWidth,
				Boolean.FALSE.equals(hovered));
		// How long is left
		int barTop = second + BUTTON_HEIGHT + 3;
		graphics.fill(x, barTop, x + contentWidth, barTop + BAR_HEIGHT, BAR_BACKGROUND);
		graphics.fill(x, barTop, x + contentWidth * Math.max(ticksLeft, 0) / DURATION_TICKS, barTop + BAR_HEIGHT, BAR_FILL);
		graphics.pose().popMatrix();
	}

	private static void drawButton(GuiGraphicsExtractor graphics, Font font, KeyMapping key, Component label, int x, int y, int width, boolean hovered) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? BUTTON_HOVERED : BUTTON, x, y, width, BUTTON_HEIGHT);
		int labelLeft = x + KEY_MARGIN + 2;
		if (!key.isUnbound()) {
			Component name = LabeledKeys.printedName(key);
			float nameWidth = (font.width(name) - GLYPH_SPACING) * SmallText.scale();
			// Square for a single letter, longer for the name of a key
			int capWidth = Math.max(KEY_SIZE, (int) Math.ceil(nameWidth) + 6);
			int capLeft = x + KEY_MARGIN;
			int capTop = y + KEY_MARGIN;
			graphics.fill(capLeft, capTop, capLeft + capWidth, capTop + KEY_SIZE, OUTLINE);
			graphics.fill(capLeft + 1, capTop + 1, capLeft + capWidth - 1, capTop + KEY_SIZE - 1, KEY);
			SmallText.left(graphics, font, name, snap(capLeft + (capWidth - nameWidth) / 2), textTop(capTop, KEY_SIZE), KEY_TEXT, false);
			labelLeft = capLeft + capWidth + 3;
		}
		SmallText.left(graphics, font, label, labelLeft, textTop(y, BUTTON_HEIGHT), LABEL);
	}

	/** Where to draw text so that its capitals are centered in the height, the font's line being taller than them. */
	private static float textTop(int top, int height) {
		return snap(top + (height - GLYPH_HEIGHT * SmallText.scale()) / 2);
	}

	/** To a whole screen pixel, for the font to stay sharp. */
	private static float snap(float position) {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		return Math.round(position * guiScale) / (float) guiScale;
	}

	/** How far off to the right the panel is, in widths: sliding in, and away once left unanswered or declined. */
	private static float slide() {
		long now = Util.getMillis();
		if (leavingAt >= 0) {
			float t = Mth.clamp((now - leavingAt) / (float) SLIDE_MILLIS, 0, 1);
			return t * t;
		}
		float t = Mth.clamp((now - shownAt) / (float) SLIDE_MILLIS, 0, 1);
		return (1 - t) * (1 - t);
	}
}
