package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.gui.components.toasts.TutorialToast;

/** The tutorial's hints, black in vanilla, in the quest tracker's hint color on the toasts' dark frame. */
@Mixin(TutorialToast.class)
public abstract class TutorialToastMixin {
	/** The quest tracker's HINT. */
	@Unique
	private static final int TEXT = 0xFFD2BBB5;

	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 0xFF000000))
	private int vanillasugar$lightText(int color) {
		return TEXT;
	}
}
