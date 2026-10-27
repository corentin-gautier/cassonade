package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.component.BundleContents;

import com.cassonade.client.storage.SlotGridTooltip;

/**
 * The wheel picks among the stacks shown in the bundle's tooltip. Vanilla counts them for its own grid,
 * four columns of whole rows, so with the nine-column grid of SlotGridTooltip it stopped short and
 * wrapped back to the first. The server only checks the index against the bundle's size.
 */
@Mixin(BundleContents.class)
public abstract class BundleContentsMixin {
	@Inject(method = "getNumberOfItemsToShow", at = @At("HEAD"), cancellable = true)
	private void cassonade$showGridItems(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(SlotGridTooltip.selectableItems(((BundleContents) (Object) this).size()));
	}
}
