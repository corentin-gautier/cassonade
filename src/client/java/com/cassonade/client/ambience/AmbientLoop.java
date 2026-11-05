package com.cassonade.client.ambience;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * An unbroken stereo loop without a position, heard all around (the sea, the wind): its volume kept here and eased toward a
 * target, for the game to read each frame; started once it has one, stopped once it is silent.
 */
final class AmbientLoop {
	final Identifier id;
	/** Volume per tick. */
	private final float ease;
	private float shown;
	private @Nullable Instance playing;

	AmbientLoop(Identifier id, float ease) {
		this.id = id;
		this.ease = ease;
	}

	void follow(Minecraft minecraft, float target) {
		shown = Mth.approach(shown, target, ease);
		if (shown > 0 && playing == null) {
			playing = new Instance(this);
			minecraft.getSoundManager().play(playing);
		} else if (shown <= 0 && playing != null) {
			playing.end();
			playing = null;
		}
	}

	float shown() {
		return shown;
	}

	boolean playing() {
		return playing != null;
	}

	private static final class Instance extends AbstractTickableSoundInstance {
		private final AmbientLoop loop;

		Instance(AmbientLoop loop) {
			super(SoundEvent.createVariableRangeEvent(loop.id), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
			this.loop = loop;
			looping = true;
			delay = 0;
			relative = true;
			attenuation = SoundInstance.Attenuation.NONE;
			volume = loop.shown;
		}

		@Override
		public void tick() {
			volume = loop.shown;
			if (Minecraft.getInstance().level == null) {
				loop.playing = null;
				loop.shown = 0;
				stop();
			}
		}

		void end() {
			stop();
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}
	}
}
