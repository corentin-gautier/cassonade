package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;

import com.cassonade.client.DescriptionText;
import com.cassonade.client.map.SmallText;

/** Tooltips' secondary lines (DescriptionText) a GUI scale step smaller than the rest of the tooltip, like the food tooltip's labels. */
@Mixin(ClientTextTooltip.class)
public abstract class ClientTextTooltipMixin {
	@Shadow
	@Final
	private FormattedCharSequence text;

	@Unique
	private boolean cassonade$description;
	@Unique
	private boolean cassonade$last;
	@Unique
	private boolean cassonade$spaced;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void cassonade$detect(FormattedCharSequence text, CallbackInfo ci) {
		cassonade$description = DescriptionText.is(text);
		cassonade$spaced = cassonade$description && DescriptionText.isSpaced(text);
		cassonade$last = cassonade$description && DescriptionText.isLast(text);
	}

	@Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
	private void cassonade$width(Font font, CallbackInfoReturnable<Integer> cir) {
		if (cassonade$description) {
			cir.setReturnValue((int) Math.ceil(font.width(text) * SmallText.scale()));
		}
	}

	@Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
	private void cassonade$height(Font font, CallbackInfoReturnable<Integer> cir) {
		if (cassonade$description) {
			int spacing = cassonade$last || cassonade$spaced ? DescriptionText.SPACING : 0;
			cir.setReturnValue((int) Math.ceil(10 * SmallText.scale()) + spacing);
		}
	}

	@Inject(method = "extractText", at = @At("HEAD"), cancellable = true)
	private void cassonade$draw(GuiGraphicsExtractor graphics, Font font, int x, int y, CallbackInfo ci) {
		if (!cassonade$description) {
			return;
		}
		SmallText.left(graphics, font, text, x, cassonade$spaced ? y + DescriptionText.SPACING : y, -1);
		ci.cancel();
	}
}
