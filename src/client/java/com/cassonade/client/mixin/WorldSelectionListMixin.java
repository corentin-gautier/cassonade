package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;

import com.cassonade.client.WorldSelectionScreen;

/** In the mod's world selection, the rows fill the panel's width, short of the scrollbar when there is one. */
@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListMixin {
	@Inject(method = "getRowWidth", at = @At("HEAD"), cancellable = true)
	private void cassonade$panelWidth(CallbackInfoReturnable<Integer> cir) {
		if (WorldSelectionScreen.styles(this)) {
			WorldSelectionList self = (WorldSelectionList) (Object) this;
			cir.setReturnValue(self.getWidth() - (self.maxScrollAmount() > 0 ? WorldSelectionScreen.SCROLLBAR_SPACE : 0));
		}
	}
}
