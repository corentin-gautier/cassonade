package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.gui.components.toasts.RecipeToast;

/**
 * Toasts sit in the minimap's dark frame (tools/panel_textures.py): the new recipes' title, purple in
 * vanilla, is written in the quest tracker's gold, and the line below, black, in its hint color.
 */
@Mixin(RecipeToast.class)
public abstract class RecipeToastMixin {
	/** The quest tracker's GOLD. */
	@Unique
	private static final int TITLE = 0xFFF6DA8A;
	/** The quest tracker's HINT. */
	@Unique
	private static final int TEXT = 0xFFD2BBB5;

	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 0xFF500050))
	private int vanillasugar$goldTitle(int color) {
		return TITLE;
	}

	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 0xFF000000))
	private int vanillasugar$lightText(int color) {
		return TEXT;
	}
}
