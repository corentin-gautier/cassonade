package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SuspendedParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Motes;
import com.cassonade.client.ambience.Swaying;

/** Vanilla's underwater specks hang still; marked by their provider (UnderwaterProviderMixin), they follow the swell as the silt does. */
@Mixin(SuspendedParticle.class)
public abstract class SuspendedParticleMixin extends SingleQuadParticle implements Swaying {
	@Unique
	private boolean cassonade$swaying;

	private SuspendedParticleMixin(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
		super(level, x, y, z, sprite);
	}

	@Override
	public void cassonade$sway() {
		cassonade$swaying = true;
	}

	@Override
	public void tick() {
		if (cassonade$swaying) {
			Vec3 swell = Motes.swell(level, x, y, z);
			xd = swell.x;
			yd = swell.y;
			zd = swell.z;
		}
		super.tick();
	}
}
