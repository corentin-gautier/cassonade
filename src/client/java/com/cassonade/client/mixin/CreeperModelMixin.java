package com.cassonade.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cassonade.client.CreeperLook;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;

/** The creeper's leaves, branches and twigs (CreeperLook), and which of them each creeper grows. */
@Mixin(CreeperModel.class)
abstract class CreeperModelMixin {
	@Unique
	private @Nullable ModelPart[] cassonade$spots;

	@Inject(method = "createBodyLayer", at = @At("HEAD"), cancellable = true)
	private static void cassonade$shape(CubeDeformation inflation, CallbackInfoReturnable<LayerDefinition> cir) {
		cir.setReturnValue(CreeperLook.bodyLayer(inflation));
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void cassonade$findSpots(ModelPart root, CallbackInfo ci) {
		cassonade$spots = CreeperLook.spots(root);
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/CreeperRenderState;)V", at = @At("TAIL"))
	private void cassonade$grow(CreeperRenderState state, CallbackInfo ci) {
		CreeperLook.grow(cassonade$spots, state);
	}
}
