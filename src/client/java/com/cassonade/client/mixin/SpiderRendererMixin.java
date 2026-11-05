package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.monster.spider.Spider;

import com.cassonade.client.ClimbingSpiders;

/** Spiders and cave spiders upright against the wall they climb (ClimbingSpiders). */
@Mixin(SpiderRenderer.class)
abstract class SpiderRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/spider/Spider;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
	private void cassonade$climb(Spider spider, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		ClimbingSpiders.extract(spider, state, partialTicks);
	}
}
