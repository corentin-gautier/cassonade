package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SuspendedParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

import com.cassonade.client.ambience.Swaying;

/** Only the underwater specks sway: the spores, from the same class, keep vanilla's fall. */
@Mixin(SuspendedParticle.UnderwaterProvider.class)
public abstract class UnderwaterProviderMixin {
	@Inject(method = "createParticle", at = @At("RETURN"))
	private void cassonade$sway(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random, CallbackInfoReturnable<Particle> cir) {
		if (cir.getReturnValue() instanceof Swaying swaying) {
			swaying.cassonade$sway();
		}
	}
}
