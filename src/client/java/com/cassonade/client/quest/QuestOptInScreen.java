package com.cassonade.client.quest;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import com.cassonade.client.LabeledKeys;
import com.cassonade.client.CassonadeClient;
import com.cassonade.client.map.FramedPanel;

/**
 * Asked once per world, the first time the player is in it: whether to follow the quest journal.
 * Either answer can be changed later in the journal.
 */
public class QuestOptInScreen extends Screen {
	private static final int MAX_WIDTH = 300;
	private static final int BORDER = FramedPanel.BORDER;
	private static final int PADDING = 10;
	private static final int BUTTON_WIDTH = 120;
	private static final int BUTTON_HEIGHT = 20;
	private static final int TITLE_SCALE = 2;
	// The theme's colors (tools/gui_theme.py)
	private static final int GOLD = 0xFFF6DA8A;
	private static final int TEXT = 0xFFE8DABC;
	private static final int MUTED = 0xFFA38883;

	private List<FormattedCharSequence> text = List.of();
	private List<FormattedCharSequence> note = List.of();

	public QuestOptInScreen() {
		super(Component.translatable("gui.cassonade.quest_opt_in.title"));
	}

	private int panelWidth() {
		return Math.min(width - 32, MAX_WIDTH);
	}

	private int panelHeight() {
		return 2 * (BORDER + PADDING) + font.lineHeight * TITLE_SCALE + 8 + text.size() * font.lineHeight + 6
				+ note.size() * font.lineHeight + 10 + BUTTON_HEIGHT;
	}

	private int panelTop() {
		return (height - panelHeight()) / 2;
	}

	@Override
	protected void init() {
		int textWidth = panelWidth() - 2 * (BORDER + PADDING);
		text = font.split(Component.translatable("gui.cassonade.quest_opt_in.text"), textWidth);
		note = font.split(Component.translatable("gui.cassonade.quest_opt_in.note", LabeledKeys.printedName(CassonadeClient.OPEN_JOURNAL)),
				textWidth);
		int buttonsTop = panelTop() + panelHeight() - BORDER - PADDING - BUTTON_HEIGHT;
		addRenderableWidget(Button.builder(Component.translatable("gui.cassonade.quest_opt_in.yes"), button -> choose(true))
				.bounds(width / 2 - BUTTON_WIDTH - 4, buttonsTop, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cassonade.quest_opt_in.no"), button -> choose(false))
				.bounds(width / 2 + 4, buttonsTop, BUTTON_WIDTH, BUTTON_HEIGHT).build());
	}

	private void choose(boolean follow) {
		QuestLog.setEnabled(follow);
		onClose();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
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
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int left = (width - panelWidth()) / 2;
		int top = panelTop();
		FramedPanel.draw(graphics, left, top, panelWidth(), panelHeight());
		int y = top + BORDER + PADDING;
		graphics.pose().pushMatrix();
		graphics.pose().translate(width / 2f, y);
		graphics.pose().scale(TITLE_SCALE, TITLE_SCALE);
		graphics.text(font, title, -font.width(title) / 2, 0, GOLD, true);
		graphics.pose().popMatrix();
		y += font.lineHeight * TITLE_SCALE + 8;
		int x = left + BORDER + PADDING;
		for (FormattedCharSequence line : text) {
			graphics.text(font, line, x, y, TEXT, true);
			y += font.lineHeight;
		}
		y += 6;
		for (FormattedCharSequence line : note) {
			graphics.text(font, line, x, y, MUTED, true);
			y += font.lineHeight;
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}
}
