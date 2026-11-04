package com.cassonade.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FogType;

import com.cassonade.Cassonade;
import com.cassonade.tweaks.DivingMask;

/**
 * Eyes open under water see blurred, as they do in real life: the world, not the interface, goes through a blur that grows
 * with the distance. A diving mask's glass keeps the view sharp.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Unique
	private static final Identifier UNDERWATER = Cassonade.id("underwater");

	@Shadow
	@Final
	private List<Identifier> requestedPostEffects;

	@Shadow
	@Final
	private Camera mainCamera;

	@Inject(method = "update", at = @At("TAIL"))
	private void cassonade$blurUnderWater(DeltaTracker deltaTracker, CallbackInfo ci) {
		if (this.mainCamera.getFluidInCamera() == FogType.WATER && !DivingMask.worn(this.mainCamera.entity())) {
			// First, before the game's own effects at the end of the frame
			this.requestedPostEffects.add(0, UNDERWATER);
		}
	}
}
