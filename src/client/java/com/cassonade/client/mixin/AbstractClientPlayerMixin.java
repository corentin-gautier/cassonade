package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.player.AbstractClientPlayer;

import com.cassonade.client.spyglass.ScopeZoom;

/** The spyglass's zoom, the game's own or a step further in or out (ScopeZoom). */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@ModifyConstant(method = "getFieldOfViewModifier", constant = @Constant(floatValue = 0.1F))
	private float cassonade$scopeStep(float gameZoom) {
		return ScopeZoom.fieldOfView();
	}
}
