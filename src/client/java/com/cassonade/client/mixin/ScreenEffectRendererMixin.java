package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * The flames of a burning player sit lower on the screen, under the horizon, where vanilla's rise past
 * the crosshair. The pose is copied when the geometry is submitted, so popping right after is safe.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
	private static final float FIRE_DROP = 0.2F;

	@WrapMethod(method = "submitFire")
	private static void cassonade$lowerFire(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, Operation<Void> original) {
		if (!Settings.on(Setting.LOWER_FIRE)) {
			original.call(poseStack, collector, sprite);
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.0F, -FIRE_DROP, 0.0F);
		original.call(poseStack, collector, sprite);
		poseStack.popPose();
	}
}
