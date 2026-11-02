package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

import com.cassonade.client.IsometricCamera;
import com.cassonade.client.PaintingChoice;
import com.cassonade.client.spyglass.ScopeZoom;

/** Looking through a spyglass, the scroll wheel zooms (ScopeZoom), and on a painting it picks another (PaintingChoice): the item in hand stays. */
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
		if (minecraft.player != null && PaintingChoice.scroll(minecraft, wheel)) {
			return selected;
		}
		return original.call(wheel, selected, limit);
	}

	/** The isometric view keeps the mouse free: a click goes through to the game without taking it back. */
	@Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
	private void cassonade$stayFree(CallbackInfo ci) {
		if (IsometricCamera.isActive()) {
			ci.cancel();
		}
	}
}
