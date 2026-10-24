package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.gui.components.toasts.TutorialToast;

/**
 * The tutorial's hints on the toasts' dark frame: their title, purple in vanilla, in the quest tracker's gold
 * like the other toasts, their text, black in vanilla, in its hint color.
 */
@Mixin(TutorialToast.class)
public abstract class TutorialToastMixin {
	/** The quest tracker's HINT. */
	@Unique
	private static final int TEXT = 0xFFD2BBB5;

	/** The quest tracker's GOLD. */
	@Unique
	private static final int TITLE = 0xFFF6DA8A;

	@ModifyConstant(method = "<init>", constant = @Constant(intValue = 0xFF500050))
	private int vanillasugar$goldTitle(int color) {
		return TITLE;
	}

	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 0xFF000000))
	private int vanillasugar$lightText(int color) {
		return TEXT;
	}
}
