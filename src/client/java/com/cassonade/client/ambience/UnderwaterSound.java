package com.cassonade.client.ambience;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import com.mojang.blaze3d.audio.Channel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;

import com.cassonade.client.mixin.ChannelAccessor;

/**
 * With the head under water, the world's sounds lose their highs and some of their volume, as water carries them: a low-pass
 * filter on each OpenAL source (SoundEngineMixin). Applied on the sound thread, where the OpenAL context lives. Without the
 * device's EFX extension, sounds stay as they are. The same filter also dulls what distance muffles (AmbientSound).
 * OpenAL's low-pass is a shelf at 5 kHz, so it must take almost all of the highs for the mids to dull too; a short, dark
 * reverb (EFX's underwater preset) adds the boomy tail water gives every sound.
 */
public final class UnderwaterSound {
	private static final float EASE = 0.2F;
	private static final float GAIN = 0.6F;
	private static final float GAIN_HIGH = 0.015F;
	/** How much of each sound feeds the reverb at full strength. */
	private static final float REVERB = 0.8F;
	private static volatile float strength;
	/** Made on first use; the context, and the filter with it, goes when the sound engine reloads. */
	private static int filter;
	private static int slot;
	private static int reverb;

	private UnderwaterSound() {
	}

	/** On the render thread, from the sound engine's tick. Tells whether the sources playing must follow. */
	public static boolean tick(Minecraft minecraft) {
		boolean under = minecraft.level != null && minecraft.gameRenderer.mainCamera().getFluidInCamera() == FogType.WATER;
		float next = Mth.approach(strength, under ? 1 : 0, EASE);
		boolean changed = next != strength;
		strength = next;
		return changed;
	}

	public static float strength() {
		return strength;
	}

	/** The music, the menus and the underwater ambience, itself recorded under water, stay clear. */
	public static boolean affects(SoundInstance instance) {
		SoundSource source = instance.getSource();
		return source != SoundSource.MASTER && source != SoundSource.MUSIC && source != SoundSource.UI
				&& !instance.getIdentifier().getPath().startsWith("ambient.underwater");
	}

	/** Whether a sound about to start needs the filter at all. */
	public static boolean filtered(SoundInstance instance) {
		return affects(instance) && (strength > 0 || AmbientSound.highs(instance) < 1);
	}

	/** On the sound thread. A source keeps the filter's values from when it was set, so the one filter serves them all. */
	public static void apply(Channel channel, SoundInstance instance) {
		if (!AL.getCapabilities().ALC_EXT_EFX) {
			return;
		}
		int source = ((ChannelAccessor) channel).cassonade$getSource();
		float now = strength;
		float highs = AmbientSound.highs(instance);
		if (now == 0 && highs == 1) {
			AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
			if (slot != 0) {
				AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, EXTEfx.AL_EFFECTSLOT_NULL, 0, EXTEfx.AL_FILTER_NULL);
			}
			return;
		}
		// 0 is OpenAL's own "no filter", which it counts as a filter
		if (filter == 0 || !EXTEfx.alIsFilter(filter)) {
			filter = EXTEfx.alGenFilters();
			EXTEfx.alFilteri(filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
		}
		EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, Mth.lerp(now, 1, GAIN));
		EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, Mth.lerp(now, 1, GAIN_HIGH) * highs);
		AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);
		reverb(source, now);
	}

	/** The send carries the distance's filter too, so a far sound echoes as dull as it sounds. */
	private static void reverb(int source, float now) {
		if (slot == 0 || !EXTEfx.alIsAuxiliaryEffectSlot(slot)) {
			slot = EXTEfx.alGenAuxiliaryEffectSlots();
			reverb = EXTEfx.alGenEffects();
			EXTEfx.alEffecti(reverb, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
			// EFX_REVERB_PRESET_UNDERWATER
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DENSITY, 0.3645F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DIFFUSION, 1);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_GAIN, 0.3162F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_GAINHF, 0.01F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DECAY_TIME, 1.49F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DECAY_HFRATIO, 0.1F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, 0.5963F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_REFLECTIONS_DELAY, 0.007F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, 7.0795F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_LATE_REVERB_DELAY, 0.011F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_AIR_ABSORPTION_GAINHF, 0.9943F);
			EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, reverb);
		}
		// One slot for every source: its gain follows the water as the sources are updated
		EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, now * REVERB);
		AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, now > 0 ? slot : EXTEfx.AL_EFFECTSLOT_NULL, 0, filter);
	}

	/** On the sound thread: how much of the highs the filter lets through, for the test, or 1 on a device without EFX. */
	public static float filterHighs() {
		return AL.getCapabilities().ALC_EXT_EFX && filter != 0 ? EXTEfx.alGetFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF) : 1;
	}
}
