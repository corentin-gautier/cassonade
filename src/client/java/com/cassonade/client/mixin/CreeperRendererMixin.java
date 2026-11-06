package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cassonade.client.CreeperLook;

import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.world.entity.monster.Creeper;

/** Each creeper's seed, from its UUID, which the server shares: the same leaves for every player, every time. */
@Mixin(CreeperRenderer.class)
abstract class CreeperRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/Creeper;Lnet/minecraft/client/renderer/entity/state/CreeperRenderState;F)V", at = @At("TAIL"))
	private void cassonade$seed(Creeper creeper, CreeperRenderState state, float partialTicks, CallbackInfo ci) {
		state.setData(CreeperLook.SEED, creeper.getUUID().getLeastSignificantBits() ^ creeper.getUUID().getMostSignificantBits());
	}
}
