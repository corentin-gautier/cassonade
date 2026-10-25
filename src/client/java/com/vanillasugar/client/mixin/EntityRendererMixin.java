package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import com.vanillasugar.client.light.DynamicLights;

/** Entities, the player's hands included, lit by the light carried around (DynamicLights). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@ModifyReturnValue(method = "getBlockLightLevel", at = @At("RETURN"))
	private int vanillasugar$carriedLight(int light, Entity entity, BlockPos pos) {
		return Math.max(light, DynamicLights.light(pos));
	}
}
