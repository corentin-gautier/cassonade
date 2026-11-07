package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.player.AbstractClientPlayer;

import com.cassonade.client.spyglass.ScopeZoom;
import com.cassonade.tweaks.PathSpeed;

/**
 * The spyglass's zoom, the game's own or a step further in or out (ScopeZoom), and a field of view that ignores the
 * dirt paths' speed (PathSpeed).
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@ModifyConstant(method = "getFieldOfViewModifier", constant = @Constant(floatValue = 0.1F))
	private float cassonade$scopeStep(float gameZoom) {
		return ScopeZoom.fieldOfView();
	}

	@ModifyExpressionValue(method = "getFieldOfViewModifier", at = @At(value = "INVOKE", target = "getAttributeValue(Lnet/minecraft/core/Holder;)D"))
	private double cassonade$pathlessSpeed(double speed) {
		return PathSpeed.withoutBonus((AbstractClientPlayer) (Object) this, speed);
	}
}
