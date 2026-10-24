package com.vanillasugar.client.quest;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.Nullable;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.LabeledKeys;
import com.vanillasugar.client.VanillaSugarClient;
import com.vanillasugar.client.map.FramedPanel;
import com.vanillasugar.client.map.SmallText;

/**
 * Buttons always shown in the bottom right corner of the screen: the advancements and, when the
 * player follows it, the quest journal, each with the key that opens it. They can be clicked whenever
 * the cursor is free (inventory or chat open); in game, their keys open the same screens.
 */
public final class QuestButtons {
	public static final int SIZE = 22;
	private static final int MARGIN = 4;
	private static final int GAP = 2;
	private static final int ICON_SIZE = 13;
	/** The theme's WOOD_DARK, lighter than the panel's background. */
	private static final int BACKGROUND_HOVERED = 0xFF473130;
	/** The theme's LABEL. */
	private static final int KEY_COLOR = 0xFFE8DABC;

	private enum Kind {
		ADVANCEMENTS("advancements"),
		JOURNAL("journal");

		final Identifier icon;

		Kind(String name) {
			this.icon = VanillaSugar.id("hud/button_" + name);
		}
	}

	private QuestButtons() {
	}

	/** The buttons shown, from the right. */
	private static List<Kind> shown() {
		List<Kind> kinds = new ArrayList<>();
		kinds.add(Kind.ADVANCEMENTS);
		if (QuestLog.isEnabled()) {
			kinds.add(Kind.JOURNAL);
		}
		return kinds;
	}

	private static int x(int guiWidth, int index) {
		return guiWidth - MARGIN - SIZE - index * (SIZE + GAP);
	}

	private static int y(int guiHeight) {
		return guiHeight - MARGIN - SIZE;
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.getDebugOverlay().showDebugScreen()) {
			return;
		}
		List<Kind> kinds = shown();
		for (int i = 0; i < kinds.size(); i++) {
			draw(graphics, kinds.get(i), x(graphics.guiWidth(), i), y(graphics.guiHeight()), false);
		}
	}

	private static void draw(GuiGraphicsExtractor graphics, Kind kind, int x, int y, boolean hovered) {
		FramedPanel.draw(graphics, x, y, SIZE, SIZE);
		if (hovered) {
			graphics.fill(x + FramedPanel.BORDER, y + FramedPanel.BORDER, x + SIZE - FramedPanel.BORDER, y + SIZE - FramedPanel.BORDER, BACKGROUND_HOVERED);
		}
		int offset = (SIZE - ICON_SIZE) / 2;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, kind.icon, x + offset, y + offset, ICON_SIZE, ICON_SIZE);
		// The key that opens it, in the bottom right corner like an item count
		Minecraft minecraft = Minecraft.getInstance();
		KeyMapping mapping = kind == Kind.JOURNAL ? VanillaSugarClient.OPEN_JOURNAL : minecraft.options.keyAdvancements;
		if (!mapping.isUnbound()) {
			Component key = LabeledKeys.printedName(mapping);
			int keyWidth = SmallText.width(minecraft.font, key);
			SmallText.left(graphics, minecraft.font, key, x + SIZE - 3 - keyWidth, y + SIZE - 3 - SmallText.lineHeight(minecraft.font), KEY_COLOR);
		}
	}

	/**
	 * Draws the buttons above a screen that frees the cursor, its panels included (recipe book,
	 * backpack), hovered under the cursor.
	 */
	public static void drawOver(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		List<Kind> kinds = shown();
		for (int i = 0; i < kinds.size(); i++) {
			int x = x(graphics.guiWidth(), i);
			int y = y(graphics.guiHeight());
			draw(graphics, kinds.get(i), x, y, mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE);
		}
	}

	/** Whether the screen frees the cursor over the HUD: a container's or the chat. */
	public static boolean freesCursor(Screen screen) {
		return screen instanceof AbstractContainerScreen || screen instanceof ChatScreen;
	}

	/**
	 * Adds the clickable buttons over those of the HUD to a screen that frees the cursor: when it opens,
	 * and again when it rebuilds its widgets (ScreenMixin), as the inventory does for the recipe book
	 * and the backpack.
	 */
	public static void addTo(Screen screen) {
		if (!freesCursor(screen)) {
			return;
		}
		List<Kind> kinds = shown();
		for (int i = 0; i < kinds.size(); i++) {
			Screens.getWidgets(screen).add(new QuestButton(kinds.get(i), x(screen.width, i), y(screen.height)));
		}
	}

	public static void openAdvancements(Minecraft minecraft) {
		minecraft.gui.setScreen(new AdvancementsListScreen(null));
	}

	private static class QuestButton extends Button {
		private final Kind kind;

		QuestButton(Kind kind, int x, int y) {
			super(x, y, SIZE, SIZE, tooltip(kind), button -> open(kind), DEFAULT_NARRATION);
			this.kind = kind;
			setTooltip(Tooltip.create(getMessage()));
		}

		private static Component tooltip(Kind kind) {
			return switch (kind) {
				case ADVANCEMENTS -> Component.translatable("gui.advancements");
				case JOURNAL -> Component.translatable("gui.vanillasugar.journal");
			};
		}

		private static void open(Kind kind) {
			Minecraft minecraft = Minecraft.getInstance();
			switch (kind) {
				case ADVANCEMENTS -> openAdvancements(minecraft);
				case JOURNAL -> minecraft.gui.setScreen(new QuestJournalScreen());
			}
		}

		/** Left out of the arrow and tab keys, which the chat keeps for its history and suggestions. */
		@Override
		public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
			return null;
		}

		/** Not focused once clicked either: enter still sends the message typed. */
		@Override
		public boolean shouldTakeFocusAfterInteraction() {
			return false;
		}

		/** Drawn by drawOver, above everything the screen draws. */
		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		}
	}
}
