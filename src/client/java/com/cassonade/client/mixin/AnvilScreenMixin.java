package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;

import com.cassonade.client.map.SmallText;

/**
 * The anvil shows its cost however high, in red when the player lacks the levels, instead of "Too Expensive!".
 * The cost is written like a tooltip, in small text inside the tooltips' frame, in the gold of the
 * enchanting table's costs rather than in bright green on a dark box.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {
	// Vanilla's green of an affordable cost, and the mod's gold (see EnchantmentScreenMixin)
	@Unique
	private static final int VANILLA_COST = 0xFF80FF20;
	@Unique
	private static final int COST_COLOR = 0xFFFFD76A;
	@Unique
	private static final int CAPITAL_HEIGHT = 7;
	/** Half the height of the line vanilla writes the cost on, below the y it writes it at. */
	@Unique
	private static final int VANILLA_LINE_MIDDLE = 4;
	/** How much lower than vanilla's line, so the frame leaves room under the slots. */
	@Unique
	private static final int BELOW_SLOTS = 3;
	/** From the window's right edge to the text's: the inventory's margin, the tooltip's frame and padding. */
	@Unique
	private static final int COST_RIGHT = 8 + 1 + 6;

	private AnvilScreenMixin(AnvilMenu menu, Inventory inventory, Component title, Identifier background) {
		super(menu, inventory, title, background);
	}

	@ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40))
	private int cassonade$noTooExpensive(int cap) {
		return Integer.MAX_VALUE;
	}

	@Redirect(method = "extractLabels", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
	private void cassonade$noCostBox(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
	}

	/** In a tooltip's frame, a size smaller (see SmallText), centered on the line vanilla writes it on. */
	@Redirect(method = "extractLabels", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private void cassonade$costTooltip(GuiGraphicsExtractor graphics, Font font, Component line, int x, int y, int color) {
		float scale = SmallText.scale();
		int width = SmallText.width(font, line);
		// The capitals' height, 7 pixels, without the spacing below the line
		int height = Mth.ceil(CAPITAL_HEIGHT * scale);
		int left = imageWidth - COST_RIGHT - width;
		int top = y + VANILLA_LINE_MIDDLE + BELOW_SLOTS - height / 2;
		TooltipRenderUtil.extractTooltipBackground(graphics, left, top, width, height, null);
		graphics.pose().pushMatrix();
		graphics.pose().translate(left, top);
		graphics.pose().scale(scale, scale);
		graphics.text(font, line, 0, 0, color == VANILLA_COST ? COST_COLOR : color);
		graphics.pose().popMatrix();
	}
}
