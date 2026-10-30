package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import com.cassonade.client.loading.WorldLoading;

/** The chunk view does not say where it is centered, which the loading screen's map needs. */
@Mixin(LevelLoadTracker.class)
abstract class LevelLoadTrackerMixin {
	@Inject(method = "updateFocus", at = @At("HEAD"))
	private void cassonade$focus(ResourceKey<Level> dimension, ChunkPos chunkPos, CallbackInfo info) {
		WorldLoading.focus((LevelLoadTracker) (Object) this, dimension, chunkPos);
	}
}
