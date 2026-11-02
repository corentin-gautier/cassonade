package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.UniformType;

import net.minecraft.client.renderer.BindGroupLayouts;

/** Gives every terrain pipeline the sky behind it (SkyBehindTerrain). */
@Mixin(BindGroupLayouts.class)
abstract class BindGroupLayoutsMixin {
	@WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/pipeline/BindGroupLayout$Builder;withUniform(Ljava/lang/String;Lcom/mojang/renderpearl/api/pipeline/UniformType;)Lcom/mojang/renderpearl/api/pipeline/BindGroupLayout$Builder;"))
	private static BindGroupLayout.Builder cassonade$addSky(BindGroupLayout.Builder builder, String name, UniformType type, Operation<BindGroupLayout.Builder> original) {
		BindGroupLayout.Builder result = original.call(builder, name, type);
		if (name.equals("TerrainUniform")) {
			result = original.call(result, "SkySampler", UniformType.COMBINED_IMAGE_SAMPLER);
			result = original.call(result, "SkyAboveSampler", UniformType.COMBINED_IMAGE_SAMPLER);
		}
		return result;
	}
}
