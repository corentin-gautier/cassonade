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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;

import com.cassonade.client.SmallTooltip;

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
	/** Half the height of the line vanilla writes the cost on, below the y it writes it at. */
	@Unique
	private static final int VANILLA_LINE_MIDDLE = 4;
	/** How much lower than vanilla's line, so the frame leaves room under the slots. */
	@Unique
	private static final int BELOW_SLOTS = 3;
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

	/** In a tooltip's frame, the same small size as the other container information (SmallTooltip). */
	@Redirect(method = "extractLabels", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private void cassonade$costTooltip(GuiGraphicsExtractor graphics, Font font, Component line, int x, int y, int color) {
		SmallTooltip.draw(graphics, font, line, imageWidth - SmallTooltip.RIGHT, y + VANILLA_LINE_MIDDLE + BELOW_SLOTS,
				color == VANILLA_COST ? COST_COLOR : color);
	}
}
