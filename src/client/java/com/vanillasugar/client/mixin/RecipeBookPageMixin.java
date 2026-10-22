package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.network.chat.Component;

/** The recipe book's pages are paper (tools/recipe_book_textures.py): page numbers are written in ink. */
@Mixin(RecipeBookPage.class)
public abstract class RecipeBookPageMixin {
	@Unique
	private static final int INK = 0xFF3A2A18;

	@Redirect(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private void vanillasugar$inkPageNumbers(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
		graphics.text(font, text, x, y, INK, false);
	}
}
