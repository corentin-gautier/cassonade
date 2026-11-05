package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Storm;

/**
 * In a blizzard, the game's falling snow gives way to the storm's flakes blown on the wind: a share of its columns left out,
 * each column by its own place so the same ones stay, until none is left.
 */
@Mixin(WeatherEffectRenderer.class)
abstract class WeatherEffectRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void cassonade$blizzard(ClientLevel level, float partialTicks, Vec3 cameraPos, WeatherRenderState renderState, CallbackInfo ci) {
		float gone = Storm.snowTakenOver();
		if (gone >= 1) {
			renderState.snowColumns.clear();
		} else if (gone > 0) {
			renderState.snowColumns.removeIf(column -> (Mth.murmurHash3Mixer(column.x() * 31 + column.z()) & 0xFFFF) / 65536.0F < gone);
		}
	}
}
