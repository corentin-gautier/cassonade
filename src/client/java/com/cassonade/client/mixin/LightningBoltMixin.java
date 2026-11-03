package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

import com.cassonade.client.ambience.Thunder;

/** The rumble of a bolt comes late and dull from afar, its crack nearby as in vanilla. */
@Mixin(LightningBolt.class)
abstract class LightningBoltMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
	private void cassonade$delayThunder(Level level, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch,
			boolean distanceDelay, Operation<Void> original) {
		if (level.isClientSide() && sound == SoundEvents.LIGHTNING_BOLT_THUNDER) {
			Thunder.play(x, y, z, sound, source, volume, pitch);
		} else {
			original.call(level, x, y, z, sound, source, volume, pitch, distanceDelay);
		}
	}
}
