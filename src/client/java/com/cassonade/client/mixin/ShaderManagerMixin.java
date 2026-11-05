package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.ShaderManager;

import com.cassonade.Cassonade;

/**
 * The blur under water (GameRendererMixin) is compiled as the shaders load, not the first time the eyes go under. Compiled
 * then, the render thread waits for a background thread, and while many sections are being built (a dive into a flooded
 * cave flying fast) every background thread waits for the render thread to make room for its section: the game froze.
 */
@Mixin(ShaderManager.class)
abstract class ShaderManagerMixin {
	@Inject(method = "apply", at = @At("TAIL"))
	private void cassonade$compileUnderwaterBlur(CallbackInfo ci) {
		((ShaderManager) (Object) this).getPostChain(Cassonade.id("underwater"), LevelTargetBundle.MAIN_TARGETS);
	}
}
