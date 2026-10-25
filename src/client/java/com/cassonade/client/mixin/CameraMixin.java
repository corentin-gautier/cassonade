package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.Camera;

import com.cassonade.client.spyglass.ScopeZoom;

/** The camera keeps its field of view no narrower than the spyglass's: down to its closest step (ScopeZoom). */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@ModifyConstant(method = "tickFov", constant = @Constant(floatValue = 0.1F))
	private float cassonade$closestScopeStep(float narrowest) {
		return ScopeZoom.NARROWEST;
	}
}
