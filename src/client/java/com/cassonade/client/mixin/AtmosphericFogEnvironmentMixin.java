package com.cassonade.client.mixin;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.util.ARGB;

import com.cassonade.client.ambience.Atmosphere;
import com.cassonade.client.ambience.MorningMist;

/** Distance tints the land with the blue of the air; the morning mist brings the fog closer and paler. */
@Mixin(AtmosphericFogEnvironment.class)
abstract class AtmosphericFogEnvironmentMixin {
	@Inject(method = "setupFog", at = @At("TAIL"))
	private void cassonade$mistDistance(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (MorningMist.density() > 0) {
			fog.environmentalStart = MorningMist.start(fog.environmentalStart);
			fog.environmentalEnd = MorningMist.end(fog.environmentalEnd);
		}
	}

	@Inject(method = "getBaseColor", at = @At("RETURN"), cancellable = true)
	private void cassonade$mistColor(ClientLevel level, Camera camera, int renderDistance, float partialTicks, CallbackInfoReturnable<Vector3fc> cir) {
		Vector3fc color = Atmosphere.fog(level, camera, cir.getReturnValue(), partialTicks);
		cir.setReturnValue(MorningMist.density() > 0 ? ARGB.srgbLerp(MorningMist.pale(), color, new Vector3f(0.85F, 0.87F, 0.88F)) : color);
	}
}
