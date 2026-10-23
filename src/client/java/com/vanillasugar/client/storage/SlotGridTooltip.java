package com.vanillasugar.client.storage;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;

import org.apache.commons.lang3.math.Fraction;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.storage.ContainerPreview;

/**
 * The contents of a bundle, a shulker box or a backpack in its tooltip, in the inventory's grid: nine
 * slots a row from the top left, at most three rows with the count of the others in the last slot,
 * then how full it is on a bar in the experience bar's style. Replaces vanilla's bundle tooltip, which
 * fills its grid from the right.
 */
public class SlotGridTooltip implements ClientTooltipComponent {
	// A slot cut from the inventory's window, and the inventory's highlight; see tools/gui_style.py
	private static final Identifier SLOT = VanillaSugar.id("container/tooltip_slot");
	private static final Identifier SLOT_HIGHLIGHT_BACK = Identifier.withDefaultNamespace("container/slot_highlight_back");
	private static final Identifier SLOT_HIGHLIGHT_FRONT = Identifier.withDefaultNamespace("container/slot_highlight_front");
	private static final Identifier BAR_BACKGROUND = VanillaSugar.id("container/fill_bar_background");
	private static final Identifier BAR_PROGRESS = VanillaSugar.id("container/fill_bar_progress");
	private static final Component EMPTY = Component.translatable("item.minecraft.bundle.empty");
	private static final Component FULL = Component.translatable("item.minecraft.bundle.full");
	private static final Component BUNDLE_DESCRIPTION = Component.translatable("item.minecraft.bundle.empty.description");
	private static final int COLUMNS = 9;
	private static final int MAX_SLOTS = 3 * COLUMNS;
	private static final int SLOT_SIZE = 18;
	private static final int WIDTH = COLUMNS * SLOT_SIZE;
	private static final int BAR_HEIGHT = 5;
	private static final int MARGIN = 4;
	private static final int TEXT_COLOR = 0xFFAAAAAA;

	private final List<ItemStack> items;
	/** The bundle's item picked with the wheel, -1 for none. */
	private final int selected;
	private final Fraction fill;
	/** Under the slots, over the bar: what an empty bundle holds, or that it is empty or full. */
	private final Component text;
	private final boolean bundle;

	private SlotGridTooltip(List<ItemStack> items, int selected, Fraction fill, Component text, boolean bundle) {
		this.items = items;
		this.selected = selected;
		this.fill = fill;
		this.text = text;
		this.bundle = bundle;
	}

	public static SlotGridTooltip of(ContainerPreview preview) {
		Fraction fill = Fraction.getFraction(preview.items().size(), preview.slots());
		return new SlotGridTooltip(preview.items(), -1, fill, label(preview.items().isEmpty(), fill), false);
	}

	public static SlotGridTooltip of(BundleTooltip tooltip) {
		BundleContents contents = tooltip.contents();
		Fraction fill = contents.weight().result().orElse(Fraction.ZERO);
		List<ItemStack> items = contents.items().stream().map(ItemStackTemplate::create).toList();
		Component text = contents.isEmpty() ? BUNDLE_DESCRIPTION : label(false, fill);
		return new SlotGridTooltip(items, contents.getSelectedItemIndex(), fill, text, true);
	}

	private static Component label(boolean empty, Fraction fill) {
		return empty ? EMPTY : fill.compareTo(Fraction.ONE) >= 0 ? FULL : null;
	}

	private int shownSlots() {
		return Math.min(MAX_SLOTS, items.size());
	}

	private int gridHeight() {
		return Mth.positiveCeilDiv(shownSlots(), COLUMNS) * SLOT_SIZE;
	}

	private List<FormattedCharSequence> textLines(Font font) {
		return text == null ? List.of() : font.split(text, WIDTH);
	}

	@Override
	public int getHeight(Font font) {
		int lines = textLines(font).size();
		return gridHeight() + (items.isEmpty() ? 0 : MARGIN) + lines * font.lineHeight + (lines > 0 ? 2 : 0) + BAR_HEIGHT + MARGIN;
	}

	@Override
	public int getWidth(Font font) {
		return WIDTH;
	}

	@Override
	public boolean showTooltipWithItemInHand() {
		return bundle;
	}

	@Override
	public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
		int left = x + (w - WIDTH) / 2;
		boolean overflowing = items.size() > MAX_SLOTS;
		int shown = shownSlots();
		// Whole rows, as in the inventory: the slots after the last stack stay empty
		int cells = Mth.positiveCeilDiv(shown, COLUMNS) * COLUMNS;
		for (int i = 0; i < cells; i++) {
			int slotX = left + i % COLUMNS * SLOT_SIZE;
			int slotY = y + i / COLUMNS * SLOT_SIZE;
			if (i >= shown) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
				continue;
			}
			// The last slot counts the items left out, as on a full bundle
			if (overflowing && i == shown - 1) {
				int hidden = items.stream().skip(shown - 1).mapToInt(ItemStack::getCount).sum();
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
				graphics.centeredText(font, "+" + hidden, slotX + SLOT_SIZE / 2, slotY + 5, -1);
				continue;
			}
			ItemStack item = items.get(i);
			boolean highlighted = i == selected;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
			// The highlight of a hovered inventory slot, 24 pixels around the item
			if (highlighted) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK, slotX - 3, slotY - 3, 24, 24);
			}
			graphics.item(item, slotX + 1, slotY + 1, i);
			graphics.itemDecorations(font, item, slotX + 1, slotY + 1);
			if (highlighted) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT, slotX - 3, slotY - 3, 24, 24);
			}
		}

		int lineY = y + gridHeight() + (items.isEmpty() ? 0 : MARGIN);
		List<FormattedCharSequence> lines = textLines(font);
		for (FormattedCharSequence line : lines) {
			graphics.text(font, line, left, lineY, TEXT_COLOR);
			lineY += font.lineHeight;
		}
		int barY = lineY + (lines.isEmpty() ? 0 : 2);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_BACKGROUND, left, barY, WIDTH, BAR_HEIGHT);
		int filled = Mth.clamp(Mth.mulAndTruncate(fill, WIDTH), 0, WIDTH);
		if (filled > 0) {
			// The progress sprite cut at the fill, so its end bands stay in place
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_PROGRESS, WIDTH, BAR_HEIGHT, 0, 0, left, barY, filled, BAR_HEIGHT);
		}
		if (bundle && selected >= 0 && selected < items.size()) {
			extractSelectedName(font, graphics, x, y, w);
		}
	}

	/** The selected item's name in its own small tooltip above, as vanilla does. */
	private void extractSelectedName(Font font, GuiGraphicsExtractor graphics, int x, int y, int w) {
		ItemStack stack = items.get(selected);
		Component name = stack.getStyledHoverName();
		int textWidth = font.width(name.getVisualOrderText());
		graphics.tooltip(font, List.of(ClientTooltipComponent.create(name.getVisualOrderText())), x + w / 2 - 12 - textWidth / 2, y - 15,
				DefaultTooltipPositioner.INSTANCE, stack.get(DataComponents.TOOLTIP_STYLE), false);
	}
}
