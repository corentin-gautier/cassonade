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

import com.cassonade.client.InfoPanel;

/**
 * The anvil shows its cost however high instead of "Too Expensive!", where vanilla writes it, on a field in the mod's style.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {
	// Vanilla's green of an affordable cost; any other color means the player lacks the levels
	@Unique
	private static final int VANILLA_COST = 0xFF80FF20;

	private AnvilScreenMixin(AnvilMenu menu, Inventory inventory, Component title, Identifier background) {
		super(menu, inventory, title, background);
	}

	@ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40))
	private int cassonade$noTooExpensive(int cap) {
		return Integer.MAX_VALUE;
	}

	/** The cost stays where vanilla writes it, on a sunk field like the windows' slots instead of a black box. */
	@Redirect(method = "extractLabels", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
	private void cassonade$costField(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
		InfoPanel.sunk(graphics, x0, y0, x1, y1);
	}

	/** In the panel's gold rather than bright green; red still means the player lacks the levels. */
	@Redirect(method = "extractLabels", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private void cassonade$costText(GuiGraphicsExtractor graphics, Font font, Component line, int x, int y, int color) {
		graphics.text(font, line, x, y, color == VANILLA_COST ? InfoPanel.LABEL : color);
	}
}
