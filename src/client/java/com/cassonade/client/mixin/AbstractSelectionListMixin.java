package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;

import com.cassonade.client.WorldSelectionScreen;

/**
 * The mod's world selection sits in a panel: no vanilla background, separators or selection outline behind its
 * rows, which start at the panel's edge with the scrollbar on the right, inside it. Scrolled to the end, the last
 * card ends on the list's bottom, as the scrollbar does.
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin {
	@Inject(method = "extractListBackground", at = @At("HEAD"), cancellable = true)
	private void cassonade$noBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (WorldSelectionScreen.styles(this)) {
			ci.cancel();
		}
	}

	@Inject(method = "extractListSeparators", at = @At("HEAD"), cancellable = true)
	private void cassonade$noSeparators(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (WorldSelectionScreen.styles(this)) {
			ci.cancel();
		}
	}

	@Inject(method = "extractSelection", at = @At("HEAD"), cancellable = true)
	private void cassonade$noSelection(GuiGraphicsExtractor graphics, @Coerce Object entry, int outlineColor, CallbackInfo ci) {
		if (WorldSelectionScreen.styles(this)) {
			ci.cancel();
		}
	}

	/** Vanilla leaves 4 pixels below the last row: the 2 above the first card, the 2 of the gap after the last. */
	@Inject(method = "contentHeight", at = @At("RETURN"), cancellable = true)
	private void cassonade$lastCardOnBottom(CallbackInfoReturnable<Integer> cir) {
		if (WorldSelectionScreen.styles(this)) {
			cir.setReturnValue(cir.getReturnValueI() - 4);
		}
	}

	@Inject(method = "getRowLeft", at = @At("HEAD"), cancellable = true)
	private void cassonade$rowsFromEdge(CallbackInfoReturnable<Integer> cir) {
		if (WorldSelectionScreen.styles(this)) {
			cir.setReturnValue(((AbstractSelectionList<?>) (Object) this).getX());
		}
	}

	@Inject(method = "scrollBarX", at = @At("HEAD"), cancellable = true)
	private void cassonade$scrollbarInside(CallbackInfoReturnable<Integer> cir) {
		if (WorldSelectionScreen.styles(this)) {
			AbstractSelectionList<?> self = (AbstractSelectionList<?>) (Object) this;
			cir.setReturnValue(self.getRight() - self.scrollbarWidth());
		}
	}
}
