package com.cassonade.client.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.cassonade.client.light.ColoredLight;

/**
 * Each vertex of a section being meshed takes the color of the light in front of it, see {@link ColoredLight}.
 * Here rather than in the block renderer, as Fabric's renderer replaces vanilla's for terrain but writes its
 * vertices the same way; fluids come through here too.
 */
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin {
	@Shadow
	@Final
	private boolean blockFormat;

	@ModifyVariable(method = "addVertex(FFFIFFIIFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int cassonade$coloredLight(int color, float x, float y, float z, int sameColor, float u, float v, int overlay, int light, float nx,
			float ny, float nz) {
		return blockFormat ? ColoredLight.tint(color, x, y, z, nx, ny, nz) : color;
	}
}
