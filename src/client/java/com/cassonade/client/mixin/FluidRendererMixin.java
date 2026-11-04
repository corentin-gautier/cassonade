package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;

/**
 * A still sprite wider than 16 pixels, the water's (tools/water_textures.py), covers that many blocks: each block shows
 * the share under its position, so the sea's surface repeats every few blocks instead of every block. The top and bottom
 * faces are the only ones reading the sprite's edges, U along X and V along Z.
 */
@Mixin(FluidRenderer.class)
abstract class FluidRendererMixin {
	@WrapOperation(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getU0()F"))
	private float cassonade$u0(TextureAtlasSprite sprite, Operation<Float> original, @Local(argsOnly = true) BlockPos pos) {
		return share(sprite, original.call(sprite), sprite.getU1(), pos.getX(), 0);
	}

	@WrapOperation(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getU1()F"))
	private float cassonade$u1(TextureAtlasSprite sprite, Operation<Float> original, @Local(argsOnly = true) BlockPos pos) {
		return share(sprite, sprite.getU0(), original.call(sprite), pos.getX(), 1);
	}

	@WrapOperation(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV0()F"))
	private float cassonade$v0(TextureAtlasSprite sprite, Operation<Float> original, @Local(argsOnly = true) BlockPos pos) {
		return share(sprite, original.call(sprite), sprite.getV1(), pos.getZ(), 0);
	}

	@WrapOperation(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getV1()F"))
	private float cassonade$v1(TextureAtlasSprite sprite, Operation<Float> original, @Local(argsOnly = true) BlockPos pos) {
		return share(sprite, sprite.getV0(), original.call(sprite), pos.getZ(), 1);
	}

	/** The edge of this block's share of the sprite, from the block's coordinate along that axis; the edge itself on a sprite of one block. */
	private static float share(TextureAtlasSprite sprite, float start, float end, int coordinate, int edge) {
		int blocks = sprite.contents().width() / 16;
		if (blocks <= 1) {
			return edge == 0 ? start : end;
		}
		return start + (end - start) * (Math.floorMod(coordinate, blocks) + edge) / blocks;
	}
}
