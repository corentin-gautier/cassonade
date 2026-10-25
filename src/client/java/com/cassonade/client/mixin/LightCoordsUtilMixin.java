package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;

import com.cassonade.client.light.DynamicLights;

/** The light carried around (DynamicLights), where chunk meshes, block entities and paintings read the light. */
@Mixin(LightCoordsUtil.class)
public abstract class LightCoordsUtilMixin {
	@ModifyExpressionValue(method = "getLightCoords(Lnet/minecraft/util/LightCoordsUtil$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/LightCoordsUtil$BrightnessGetter;packedBrightness(Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/core/BlockPos;)I"))
	private static int cassonade$carriedLight(int lightCoords, @Local(argsOnly = true) BlockPos pos) {
		return DynamicLights.withCarried(lightCoords, pos);
	}
}
