package com.cassonade.client.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;

import com.cassonade.client.ambience.UnderwaterSound;

/** Sounds are muffled under water: those playing as the head goes in or out, and each new one before it starts. */
@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {
	@Shadow
	@Final
	private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

	@Inject(method = "tickInGameSound", at = @At("HEAD"))
	private void cassonade$followWater(CallbackInfo ci) {
		if (UnderwaterSound.tick(Minecraft.getInstance())) {
			instanceToChannel.forEach((instance, handle) -> {
				if (UnderwaterSound.affects(instance)) {
					handle.execute(UnderwaterSound::apply);
				}
			});
		}
	}

	@WrapOperation(method = "play", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object cassonade$muffleNew(Map<Object, Object> channels, Object instance, Object handle, Operation<Object> original) {
		// The method fills other maps too: only this one holds channels
		if (handle instanceof ChannelAccess.ChannelHandle channel && UnderwaterSound.strength() > 0 && UnderwaterSound.affects((SoundInstance) instance)) {
			channel.execute(UnderwaterSound::apply);
		}
		return original.call(channels, instance, handle);
	}
}
