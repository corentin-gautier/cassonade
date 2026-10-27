package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

import com.cassonade.client.spyglass.ScopeZoom;

/** Looking through a spyglass, the scroll wheel zooms (ScopeZoom) and the item in hand stays the spyglass. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@WrapOperation(method = "onScroll", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I"))
	private int cassonade$zoomThroughSpyglass(double wheel, int selected, int limit, Operation<Integer> original) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null && minecraft.player.isScoping()) {
			ScopeZoom.scroll(minecraft, wheel);
			return selected;
		}
		return original.call(wheel, selected, limit);
	}
}
