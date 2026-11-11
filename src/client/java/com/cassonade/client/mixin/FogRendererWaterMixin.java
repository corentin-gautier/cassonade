package com.cassonade.client.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.material.FogType;

import com.cassonade.client.dev.DevRendering;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/** Far water is dim, not brighter than the surface above it, dimmer the deeper you are and darker at night; never so dark that you could not find your way. At the end, as the game brings this color back to full brightness under water. */
@Mixin(FogRenderer.class)
abstract class FogRendererWaterMixin {
	@Unique
	private static final float NEAR_SURFACE = 0.5F;
	@Unique
	private static final float DEEPEST = 0.1F;
	@Unique
	private static final float DEEPEST_BELOW_SEA = 40;
	/** At the dead of night, how much of that color is left, as much as the surface seen from below keeps (SURFACE_NIGHT in terrain.fsh): in a real sea at night the water is all but black, yet the player has to find their way. */
	@Unique
	private static final float NIGHT = 0.12F;
	/** At the dead of night, how much of its blue the water's color loses, so that far water turns black rather than navy. */
	@Unique
	private static final float NIGHT_GREY = 0.6F;
	/** The sky's light level by day and at the dead of night. */
	@Unique
	private static final float DAY_SKY_LIGHT = 15;
	@Unique
	private static final float NIGHT_SKY_LIGHT = 4;

	@Inject(method = "computeFogColor", at = @At("TAIL"))
	private void cassonade$dimWithDepth(CallbackInfo ci, @Local(argsOnly = true) Camera camera, @Local(argsOnly = true, ordinal = 0) float partialTicks,
			@Local(argsOnly = true) ClientLevel level, @Local(argsOnly = true) Vector4f color) {
		if (camera.getFluidInCamera() == FogType.WATER && !DevRendering.vanilla() && Settings.on(Setting.UNDERWATER_VIEW)) {
			float depth = (float) (level.getSeaLevel() - camera.position().y) / DEEPEST_BELOW_SEA;
			// Read as it changes, not from the level's darkening, a whole number that made the color jump at dawn and dusk
			float skyLight = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_LEVEL, partialTicks);
			float night = Mth.clamp((DAY_SKY_LIGHT - skyLight) / (DAY_SKY_LIGHT - NIGHT_SKY_LIGHT), 0, 1);
			float dim = Mth.lerp(Mth.clamp(depth, 0, 1), NEAR_SURFACE, DEEPEST) * Mth.lerp(night, 1, NIGHT);
			float grey = (color.x() + color.y() + color.z()) / 3;
			float keep = 1 - NIGHT_GREY * night;
			color.set(Mth.lerp(keep, grey, color.x()) * dim, Mth.lerp(keep, grey, color.y()) * dim, Mth.lerp(keep, grey, color.z()) * dim, color.w());
		}
	}
}
