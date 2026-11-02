package com.cassonade.client.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;

/** Far water is dim, not brighter than the surface above it, and dimmer the deeper you are; never so dark that you could not find your way. At the end, as the game brings this color back to full brightness under water. */
@Mixin(FogRenderer.class)
abstract class FogRendererWaterMixin {
	@Unique
	private static final float NEAR_SURFACE = 0.5F;
	@Unique
	private static final float DEEPEST = 0.1F;
	@Unique
	private static final float DEEPEST_BELOW_SEA = 40;

	@Inject(method = "computeFogColor", at = @At("TAIL"))
	private void cassonade$dimWithDepth(CallbackInfo ci, @Local(argsOnly = true) Camera camera, @Local(argsOnly = true) ClientLevel level, @Local(argsOnly = true) Vector4f color) {
		if (camera.getFluidInCamera() == FogType.WATER) {
			float depth = (float) (level.getSeaLevel() - camera.position().y) / DEEPEST_BELOW_SEA;
			float dim = Mth.lerp(Mth.clamp(depth, 0, 1), NEAR_SURFACE, DEEPEST);
			color.mul(dim, dim, dim, 1);
		}
	}
}
