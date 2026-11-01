package com.cassonade.client.ambience;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
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
 * device's EFX extension, sounds stay as they are.
 */
public final class UnderwaterSound {
	private static final float EASE = 0.2F;
	private static final float GAIN = 0.75F;
	private static final float GAIN_HIGH = 0.1F;
	private static volatile float strength;
	/** Made on first use; the context, and the filter with it, goes when the sound engine reloads. */
	private static int filter;

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

	/** On the sound thread. */
	public static void apply(Channel channel) {
		if (!AL.getCapabilities().ALC_EXT_EFX) {
			return;
		}
		int source = ((ChannelAccessor) channel).cassonade$getSource();
		float now = strength;
		if (now == 0) {
			AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
			return;
		}
		// 0 is OpenAL's own "no filter", which it counts as a filter
		if (filter == 0 || !EXTEfx.alIsFilter(filter)) {
			filter = EXTEfx.alGenFilters();
			EXTEfx.alFilteri(filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
		}
		EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, Mth.lerp(now, 1, GAIN));
		EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, Mth.lerp(now, 1, GAIN_HIGH));
		AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);
	}

	/** On the sound thread: how much of the highs the filter lets through, for the test, or 1 on a device without EFX. */
	public static float filterHighs() {
		return AL.getCapabilities().ALC_EXT_EFX && filter != 0 ? EXTEfx.alGetFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF) : 1;
	}
}
