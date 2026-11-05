package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.monster.spider.Spider;

import com.cassonade.client.ClimbingSpiders;

/**
 * Spiders and cave spiders drawn smaller, their shadow with them, and upright against the wall they climb (ClimbingSpiders).
 * Only the picture: their hitbox stays the game's, so they are no harder to hit.
 */
@Mixin(SpiderRenderer.class)
abstract class SpiderRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/spider/Spider;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
	private void cassonade$climb(Spider spider, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		state.scale *= ClimbingSpiders.SCALE;
		ClimbingSpiders.extract(spider, state, partialTicks);
	}
}
