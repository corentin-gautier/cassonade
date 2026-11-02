package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cassonade.client.SkyBehindTerrain;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.commands.RenderPass;

import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

/** Binds the sky behind the terrain, for terrain.fsh. */
@Mixin(ChunkSectionsToRender.class)
abstract class ChunkSectionsToRenderMixin {
	@Inject(method = "renderLayers", at = @At("HEAD"))
	private void cassonade$bindSky(CallbackInfo ci, @Local(argsOnly = true) RenderPass renderPass) {
		SkyBehindTerrain.bind(renderPass);
	}
}
