package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.vanillasugar.client.GearWingsLayer;
import com.vanillasugar.gear.ElytraGear;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void vanillasugar$extractGearElytra(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		state.setData(GearWingsLayer.ELYTRA, entity instanceof Player player ? ElytraGear.shown(player) : ItemStack.EMPTY);
	}
}
