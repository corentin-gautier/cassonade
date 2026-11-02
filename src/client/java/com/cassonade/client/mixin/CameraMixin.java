package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;

import com.cassonade.client.IsometricCamera;
import com.cassonade.client.spyglass.ScopeZoom;

/** The camera keeps its field of view no narrower than the spyglass's: down to its closest step (ScopeZoom), and looks down from afar in the isometric view. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Shadow
	protected abstract void move(float forwards, float up, float right);

	@ModifyConstant(method = "tickFov", constant = @Constant(floatValue = 0.1F))
	private float cassonade$closestScopeStep(float narrowest) {
		return ScopeZoom.NARROWEST;
	}

	@Inject(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), cancellable = true)
	private void cassonade$lookDownFromAfar(float partialTicks, CallbackInfo ci) {
		if (IsometricCamera.isActive()) {
			this.setRotation(IsometricCamera.yaw(), IsometricCamera.PITCH);
			// Not vanilla's getMaxZoom: walls don't bring this view closer
			this.move(-IsometricCamera.DISTANCE, 0.0F, 0.0F);
			ci.cancel();
		}
	}
}
