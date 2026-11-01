package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.animal.bee.BeeModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.world.entity.animal.bee.Bee;

/** Bees drawn smaller, adults and babies, their shadow with them. Only the picture: their hitbox stays the game's. */
// The parent BeeRenderer itself has, which the game still marks as deprecated
@SuppressWarnings("deprecation")
@Mixin(BeeRenderer.class)
abstract class BeeRendererMixin extends AgeableMobRenderer<Bee, BeeRenderState, BeeModel> {
	@Unique
	private static final float CASSONADE_SCALE = 0.6F;

	private BeeRendererMixin(EntityRendererProvider.Context context, BeeModel adult, BeeModel baby, float shadow) {
		super(context, adult, baby, shadow);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void cassonade$smallerShadow(EntityRendererProvider.Context context, CallbackInfo ci) {
		shadowRadius *= CASSONADE_SCALE;
	}

	@Override
	protected void scale(BeeRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(CASSONADE_SCALE, CASSONADE_SCALE, CASSONADE_SCALE);
	}
}
