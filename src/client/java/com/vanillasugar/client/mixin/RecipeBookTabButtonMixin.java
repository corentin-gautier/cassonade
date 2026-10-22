package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;

/**
 * The selected recipe book tab holds a page (tools/recipe_book_textures.py): drawn wider than vanilla,
 * it crosses the book's cover and its paper joins the pages.
 */
@Mixin(RecipeBookTabButton.class)
public abstract class RecipeBookTabButtonMixin {
	/** From the selected tab's left edge to just inside the pages, past the cover and its shadow. */
	@Unique
	private static final int SELECTED_WIDTH = 42;

	@Shadow
	private boolean selected;

	@ModifyArg(method = "extractContents", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			index = 4)
	private int vanillasugar$reachPages(int width) {
		return selected ? SELECTED_WIDTH : width;
	}
}
