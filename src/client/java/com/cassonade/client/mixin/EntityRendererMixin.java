package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import com.cassonade.client.HiddenEntities;
import com.cassonade.client.light.DynamicLights;

/** Entities, the player's hands included, lit by the light carried around (DynamicLights); those the terrain hides not drawn (HiddenEntities). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
	@Shadow
	protected abstract boolean affectedByCulling(T entity);

	@Shadow
	protected abstract AABB getBoundingBoxForCulling(T entity, float partialTicks);

	@ModifyReturnValue(method = "shouldRender", at = @At("RETURN"))
	private boolean cassonade$skipHidden(boolean shown, T entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
		return shown && !(affectedByCulling(entity) && HiddenEntities.isHidden(entity, getBoundingBoxForCulling(entity, partialTicks)));
	}

	@ModifyReturnValue(method = "getBlockLightLevel", at = @At("RETURN"))
	private int cassonade$carriedLight(int light, Entity entity, BlockPos pos) {
		// At the entity's exact position, not the block's: rounded down, never brighter than the exact light below
		return Math.max(light, (int) DynamicLights.light(entity.getLightProbePosition(1.0F)));
	}

	/** The fractions of a level, which getBlockLightLevel cannot return, so that an entity brightens evenly as a light nears. */
	@ModifyReturnValue(method = "getPackedLightCoords", at = @At("RETURN"))
	private int cassonade$carriedLightCoords(int lightCoords, Entity entity, float partialTickTime) {
		return DynamicLights.withCarried(lightCoords, entity.getLightProbePosition(partialTickTime));
	}
}
