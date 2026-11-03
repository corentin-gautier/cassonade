package com.cassonade.client.ambience;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * One sound of the world around the player: what plays, from where, and how distance tells on it. Without the game's attenuation,
 * a straight line that dies out at a fixed reach: placed by ear from its position, its volume follows the distance here instead.
 */
public final class AmbientSound {
	private static final RandomSource RANDOM = RandomSource.create();
	private final Identifier sound;
	private final float volume;
	private float pitch = 1;
	private @Nullable Vec3 at;
	private float around;
	private float above;
	private float range;
	private boolean muffled;

	private AmbientSound(Identifier sound, float volume) {
		this.sound = sound;
		this.volume = volume;
	}

	public static AmbientSound of(Identifier sound, float volume) {
		return new AmbientSound(sound, volume);
	}

	/**
	 * Squared: the ear hears a straight line of gain as nearly flat until it dies out, and each step closer should tell.
	 * 1 at the source, 0 from its range on.
	 */
	public static float closeness(float distance, float range) {
		float left = Mth.clamp(1 - distance / range, 0, 1);
		return left * left;
	}

	public AmbientSound pitch(float pitch) {
		this.pitch = pitch;
		return this;
	}

	/** At this place in the world. */
	public AmbientSound from(Vec3 at) {
		this.at = at;
		return this;
	}

	/** At this distance from the player, in a random direction, this much above the eyes. */
	public AmbientSound around(float distance, float height) {
		around = distance;
		above = height;
		return this;
	}

	/** Fainter with distance, silent from this range on; otherwise as loud from anywhere. */
	public AmbientSound fading(float range) {
		this.range = range;
		return this;
	}

	/** Duller with distance too, as the air takes the highs first. Needs a range. */
	public AmbientSound muffled() {
		muffled = true;
		return this;
	}

	/** The sound started, or null when too far to be heard. */
	public @Nullable SoundInstance play(Minecraft minecraft) {
		Vec3 eye = minecraft.player.getEyePosition();
		float angle = RANDOM.nextFloat() * Mth.TWO_PI;
		Vec3 from = at != null ? at : eye.add(Mth.cos(angle) * around, above, Mth.sin(angle) * around);
		float distance = (float) from.distanceTo(eye);
		float closeness = range > 0 ? closeness(distance, range) : 1;
		if (closeness <= 0) {
			return null;
		}
		SoundInstance instance = new Instance(sound, volume * closeness, pitch, from, muffled ? 1 - distance / range : 1);
		minecraft.getSoundManager().play(instance);
		return instance;
	}

	/** How much of a sound's highs distance leaves, whatever the water takes on top (UnderwaterSound). */
	static float highs(SoundInstance instance) {
		return instance instanceof Dulled dulled ? dulled.highs() : 1;
	}

	/** A sound that distance dulled, as these and the thunder (Thunder). */
	interface Dulled {
		float highs();
	}

	private static final class Instance extends SimpleSoundInstance implements Dulled {
		private final float highs;

		Instance(Identifier sound, float volume, float pitch, Vec3 from, float highs) {
			super(sound, SoundSource.AMBIENT, volume, pitch, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, from.x, from.y, from.z, false);
			this.highs = highs;
		}

		@Override
		public float highs() {
			return highs;
		}
	}
}
