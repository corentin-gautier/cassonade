package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;

import com.cassonade.client.FixedOnScreen;
import com.cassonade.client.OpenFade;

/** Undoes the window opening's slide, zoom and fade for the widgets that are part of the HUD (see ScreenMixin). */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
	@Unique
	private float cassonade$fade;

	@Unique
	private boolean cassonade$fixed;

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void cassonade$fixedStart(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		cassonade$fixed = this instanceof FixedOnScreen && OpenFade.alpha() < 1f;
		if (cassonade$fixed) {
			cassonade$fade = OpenFade.alpha();
			OpenFade.set(1f);
			graphics.pose().pushMatrix();
			graphics.pose().identity();
		}
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void cassonade$fixedEnd(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (cassonade$fixed) {
			graphics.pose().popMatrix();
			OpenFade.set(cassonade$fade);
			cassonade$fixed = false;
		}
	}
}
