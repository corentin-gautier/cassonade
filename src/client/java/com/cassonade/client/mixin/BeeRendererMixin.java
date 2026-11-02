package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.world.entity.animal.bee.Bee;

/** Bees drawn smaller, adults and babies, their shadow with them. Only the picture: their hitbox stays the game's. */
@Mixin(BeeRenderer.class)
abstract class BeeRendererMixin {
	@Unique
	private static final float CASSONADE_SCALE = 0.4F;

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/animal/bee/Bee;Lnet/minecraft/client/renderer/entity/state/BeeRenderState;F)V", at = @At("TAIL"))
	private void cassonade$smaller(Bee bee, BeeRenderState state, float partialTicks, CallbackInfo ci) {
		state.scale *= CASSONADE_SCALE;
	}
}
