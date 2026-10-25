package com.cassonade.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import com.cassonade.client.map.SmallText;
import com.cassonade.client.quest.QuestTracker;

/**
 * What the player just picked up, listed discreetly on the right of the screen, halfway down and below the
 * minimap and the quest followed: the item, how many and its name, the newest at the bottom. Picking up
 * more of an item still listed adds to its line. A line stays three seconds, then slides out to the right.
 * Client only: the server tells every client what a player picks up, mod or not.
 */
public final class PickedUpItems {
	private static final int SHOWN = 60;
	private static final int LEAVING = 8;
	private static final int MAX_LINES = 6;
	/** Items three quarters of their size in the inventory, so the list stays discreet. */
	private static final float ICON_SCALE = 0.75F;
	private static final int ICON = 12;
	private static final int LINE = 13;
	private static final int MARGIN = 4;
	private static final int TEXT_GAP = 3;
	private static final int TEXT_COLOR = 0xFFFFFF;

	private static final List<Line> lines = new ArrayList<>();

	private static final class Line {
		final ItemStack stack;
		int count;
		int age;

		Line(ItemStack stack, int count) {
			this.stack = stack;
			this.count = count;
		}
	}

	private PickedUpItems() {
	}

	public static void add(ItemStack picked, int amount) {
		if (picked.isEmpty() || amount <= 0) {
			return;
		}
		for (Iterator<Line> it = lines.iterator(); it.hasNext(); ) {
			Line line = it.next();
			if (ItemStack.isSameItemSameComponents(line.stack, picked)) {
				it.remove();
				line.count += amount;
				line.age = 0;
				lines.add(line);
				return;
			}
		}
		lines.add(new Line(picked.copyWithCount(1), amount));
		if (lines.size() > MAX_LINES) {
			lines.removeFirst();
		}
	}

	/** The lines shown, oldest first, as their text: for the tests. */
	public static List<String> shown() {
		return lines.stream().map(line -> text(line).getString()).toList();
	}

	public static void tick(Minecraft minecraft) {
		if (minecraft.player == null) {
			lines.clear();
			return;
		}
		lines.removeIf(line -> ++line.age > SHOWN + LEAVING);
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (lines.isEmpty() || minecraft.player == null || minecraft.player.isSpectator()) {
			return;
		}
		Font font = minecraft.font;
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		int top = Math.max(QuestTracker.bottom(minecraft) + MARGIN, graphics.guiHeight() / 2 - lines.size() * LINE / 2);
		int iconX = graphics.guiWidth() - MARGIN - ICON;
		for (int i = 0; i < lines.size(); i++) {
			Line line = lines.get(i);
			float leaving = Mth.clamp((line.age + partialTick - SHOWN) / LEAVING, 0, 1);
			Component text = text(line);
			int textWidth = SmallText.width(font, text);
			float x = iconX + leaving * (MARGIN + ICON + TEXT_GAP + textWidth);
			int y = top + i * LINE;
			graphics.pose().pushMatrix();
			graphics.pose().translate(x, y);
			graphics.pose().scale(ICON_SCALE, ICON_SCALE);
			graphics.item(line.stack, 0, 0);
			graphics.pose().popMatrix();
			SmallText.left(graphics, font, text, x - TEXT_GAP - textWidth, y + (ICON - SmallText.lineHeight(font)) / 2f,
					ARGB.color(1 - leaving, TEXT_COLOR));
		}
	}

	private static Component text(Line line) {
		return Component.translatable("gui.cassonade.picked_up", line.count, line.stack.getHoverName());
	}
}
