package com.cassonade.client.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cassonade.client.SkyBehindTerrain;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.renderer.LevelRenderer;

/** Empties the copies of the sky before each frame, which may draw none. */
@Mixin(LevelRenderer.class)
abstract class LevelRendererSkyMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void cassonade$prepareSky(CallbackInfo ci, @Local(argsOnly = true) Vector4f clearColor) {
		SkyBehindTerrain.beginFrame(clearColor);
	}
}
