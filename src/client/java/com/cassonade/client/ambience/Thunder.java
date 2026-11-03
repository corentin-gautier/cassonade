package com.cassonade.client.ambience;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * A lightning bolt's rumble reaches the ear after its flash, the later and duller the farther it struck, as the air takes the
 * highs first (LightningBoltMixin). Its sharp crack nearby stays at once.
 */
public final class Thunder {
	/** Slower than sound really goes, as vanilla's fireworks are: a bolt never strikes more than a few hundred blocks away, and the wait should tell. */
	private static final double BLOCKS_PER_SECOND = 40;
	/** From this distance on, the rumble keeps only what is left of its highs. */
	private static final float DULL_RANGE = 256;
	private static final float FAR_HIGHS = 0.15F;

	private Thunder() {
	}

	public static void play(double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch) {
		Minecraft minecraft = Minecraft.getInstance();
		double distance = Math.sqrt(minecraft.gameRenderer.mainCamera().position().distanceToSqr(x, y, z));
		float highs = Mth.lerp(Mth.clamp((float) distance / DULL_RANGE, 0, 1), 1, FAR_HIGHS);
		SoundInstance rumble = new Rumble(sound, source, volume, pitch, x, y, z, highs);
		int delay = (int) (distance / BLOCKS_PER_SECOND * 20);
		if (delay > 0) {
			minecraft.getSoundManager().playDelayed(rumble, delay);
		} else {
			minecraft.getSoundManager().play(rumble);
		}
	}

	private static final class Rumble extends SimpleSoundInstance implements AmbientSound.Dulled {
		private final float highs;

		Rumble(SoundEvent sound, SoundSource source, float volume, float pitch, double x, double y, double z, float highs) {
			super(sound, source, volume, pitch, SoundInstance.createUnseededRandom(), x, y, z);
			this.highs = highs;
		}

		@Override
		public float highs() {
			return highs;
		}
	}
}
