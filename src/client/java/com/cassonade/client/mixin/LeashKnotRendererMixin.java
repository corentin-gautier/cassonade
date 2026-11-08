package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LeashKnotRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import com.cassonade.client.stable.PostKnot;

/** On a hitching post, the knot lies around the rail (PostKnot); a knot holds nothing else to draw. */
@Mixin(LeashKnotRenderer.class)
abstract class LeashKnotRendererMixin {
	@Inject(method = "submit", at = @At("HEAD"), cancellable = true)
	private void cassonade$onPost(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (PostKnot.submit(state, poseStack, collector)) {
			ci.cancel();
		}
	}
}
