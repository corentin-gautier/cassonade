package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.CameraType;

import com.cassonade.client.IsometricCamera;

/** Adds the isometric view to what F5 cycles through. */
@Mixin(CameraType.class)
abstract class CameraTypeMixin {
	@Inject(method = "cycle", at = @At("HEAD"), cancellable = true)
	private void cassonade$isometricView(CallbackInfoReturnable<CameraType> cir) {
		CameraType next = IsometricCamera.cycle((CameraType) (Object) this);
		if (next != null) {
			cir.setReturnValue(next);
		}
	}
}
