package com.cassonade.client.ambience;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.SOFTSourceSpatialize;
import org.lwjgl.openal.EXTEfx;

import com.mojang.blaze3d.audio.Channel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;

import com.cassonade.client.mixin.ChannelAccessor;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * With the head under water, the world's sounds lose their highs and some of their volume, as water carries them, on each
 * OpenAL source (SoundEngineMixin). Applied on the sound thread, where the OpenAL context lives. Without the device's EFX
 * extension, sounds stay as they are. The direct path's low-pass also dulls what distance muffles (AmbientSound).
 * OpenAL's low-pass has no cutoff, only a shelf at 5 kHz that leaves the mids in: under water the sound goes instead through
 * an equalizer on an effect slot, the direct path fading out, with bands that take the mids from 1 kHz down as the skull,
 * which hears for the ears there, does. Hearing through the skull, one cannot tell where a sound comes from: the sources are
 * no longer panned. The surface sends back down nearly all that comes from the air, so those sounds are much fainter. Open
 * water has nothing to echo from; only shut in, under a ceiling of blocks, a short and dull reverb on a second send.
 * Out of the water, the same filter dulls the rain under a roof (Shelter), and the first send rings in a cave (CaveEcho).
 */
public final class UnderwaterSound {
	private static final float EASE = 0.2F;
	private static final float GAIN = 0.6F;
	/** What the send's own shelf leaves above 5 kHz, under the equalizer's cut. */
	private static final float GAIN_HIGH = 0.01F;
	/** The equalizer's bands: lows kept, mids from 1.5 kHz and highs from 4 kHz down by its most, -18 dB each. */
	private static final float LOW_CUTOFF = 250;
	private static final float LOW_GAIN = 1.2F;
	private static final float MID_CENTER = 1500;
	private static final float MID_GAIN = 0.126F;
	private static final float HIGH_CUTOFF = 4000;
	private static final float HIGH_GAIN = 0.126F;
	/** A sound from above the surface: some 30 dB in reality, less here so that a creeper on the bank is still heard. */
	private static final float FROM_AIR = 0.25F;
	/** How far up a ceiling shuts the water in, and how much of each sound then feeds the reverb. */
	private static final int CEILING_REACH = 12;
	private static final float REVERB = 0.5F;
	/** Ticks between two looks at the ceiling, and the ease from open to shut in, slower than the water's. */
	private static final int LOOK_EVERY = 10;
	private static final float ENCLOSED_EASE = 0.05F;
	private static volatile float strength;
	private static volatile float enclosed;
	private static boolean shut;
	private static int nextLook;
	/** Made on first use; the context, and the filters and slots with it, goes when the sound engine reloads. */
	private static int filter;
	private static int sendFilter;
	private static int slot;
	private static int equalizer;
	private static int reverbSlot;
	private static int reverb;

	private UnderwaterSound() {
	}

	/** On the render thread, from the sound engine's tick. Tells whether the sources playing must follow. */
	public static boolean tick(Minecraft minecraft) {
		boolean under = minecraft.level != null && minecraft.gameRenderer.mainCamera().getFluidInCamera() == FogType.WATER
				&& Settings.on(Setting.UNDERWATER_SOUND);
		if (--nextLook <= 0) {
			nextLook = LOOK_EVERY;
			shut = under && shutIn(minecraft.level, BlockPos.containing(minecraft.gameRenderer.mainCamera().position()));
		}
		float next = Mth.approach(strength, under ? 1 : 0, EASE);
		float nextEnclosed = Mth.approach(enclosed, shut ? 1 : 0, ENCLOSED_EASE);
		boolean changed = next != strength || nextEnclosed != enclosed;
		// Both look around each tick, whichever changed
		changed |= Shelter.tick(minecraft);
		changed |= CaveEcho.tick(minecraft);
		strength = next;
		enclosed = nextEnclosed;
		return changed;
	}

	/** Whether a block, not the open air, closes the water above the eyes. */
	private static boolean shutIn(ClientLevel level, BlockPos eye) {
		BlockPos.MutableBlockPos pos = eye.mutable();
		for (int i = 1; i <= CEILING_REACH; i++) {
			pos.move(0, 1, 0);
			if (!level.getFluidState(pos).is(FluidTags.WATER)) {
				return !level.getBlockState(pos).isAir();
			}
		}
		return true;
	}

	public static float strength() {
		return strength;
	}

	public static float enclosed() {
		return enclosed;
	}

	/** The music, the menus and the underwater ambience, itself recorded under water, stay clear. */
	public static boolean affects(SoundInstance instance) {
		SoundSource source = instance.getSource();
		return source != SoundSource.MASTER && source != SoundSource.MUSIC && source != SoundSource.UI
				&& !instance.getIdentifier().getPath().startsWith("ambient.underwater");
	}

	/** Whether a sound about to start needs the filter at all. */
	public static boolean filtered(SoundInstance instance) {
		return affects(instance) && (strength > 0 || CaveEcho.echo() > 0 || highs(instance) < 1);
	}

	/** What distance and a roof leave of a sound's highs, before the water. */
	private static float highs(SoundInstance instance) {
		return AmbientSound.highs(instance) * Shelter.highs(instance);
	}

	/**
	 * On the render thread, where the world is read: whether a sound comes from above the water. One around the player, as the
	 * sea's loops, is the world outside too.
	 */
	public static boolean fromAir(SoundInstance instance) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || strength == 0) {
			return false;
		}
		return instance.isRelative() || !minecraft.level.getFluidState(BlockPos.containing(instance.getX(), instance.getY(), instance.getZ())).is(FluidTags.WATER);
	}

	/** On the sound thread. A source keeps a filter's values from when it was set, so each filter serves them all. */
	public static void apply(Channel channel, SoundInstance instance, boolean fromAir) {
		if (!AL.getCapabilities().ALC_EXT_EFX) {
			return;
		}
		int source = ((ChannelAccessor) channel).cassonade$getSource();
		float now = strength;
		float highs = highs(instance);
		spatialize(source, now);
		if (now == 0 && highs == 1 && CaveEcho.echo() == 0) {
			AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
			if (slot != 0 || CaveEcho.made()) {
				AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, EXTEfx.AL_EFFECTSLOT_NULL, 0, EXTEfx.AL_FILTER_NULL);
			}
			if (reverbSlot != 0) {
				AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, EXTEfx.AL_EFFECTSLOT_NULL, 1, EXTEfx.AL_FILTER_NULL);
			}
			return;
		}
		// 0 is OpenAL's own "no filter", which it counts as a filter
		if (filter == 0 || !EXTEfx.alIsFilter(filter)) {
			filter = lowpass();
			sendFilter = lowpass();
		}
		// The direct path keeps what distance muffles and fades out under water, as the equalized send fades in
		EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, 1 - now);
		EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, highs);
		AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);
		EXTEfx.alFilterf(sendFilter, EXTEfx.AL_LOWPASS_GAIN, now * GAIN * Mth.lerp(fromAir ? now : 0, 1, FROM_AIR));
		EXTEfx.alFilterf(sendFilter, EXTEfx.AL_LOWPASS_GAINHF, GAIN_HIGH * highs);
		if (now > 0) {
			equalized(source, now);
		} else {
			CaveEcho.send(source);
		}
		reverberated(source, now);
	}

	/** Not panned once well under, so heard from everywhere; distance still fades it. Needs OpenAL Soft's extension. */
	private static void spatialize(int source, float now) {
		if (AL.getCapabilities().AL_SOFT_source_spatialize) {
			AL10.alSourcei(source, SOFTSourceSpatialize.AL_SOURCE_SPATIALIZE_SOFT, now > 0.5F ? AL10.AL_FALSE : SOFTSourceSpatialize.AL_AUTO_SOFT);
		}
	}

	private static int lowpass() {
		int made = EXTEfx.alGenFilters();
		EXTEfx.alFilteri(made, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
		return made;
	}

	/** One slot for every source, the send's filter giving each its share. */
	private static void equalized(int source, float now) {
		if (slot == 0 || !EXTEfx.alIsAuxiliaryEffectSlot(slot)) {
			slot = EXTEfx.alGenAuxiliaryEffectSlots();
			equalizer = EXTEfx.alGenEffects();
			EXTEfx.alEffecti(equalizer, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EQUALIZER);
			EXTEfx.alEffectf(equalizer, EXTEfx.AL_EQUALIZER_LOW_CUTOFF, LOW_CUTOFF);
			EXTEfx.alEffectf(equalizer, EXTEfx.AL_EQUALIZER_LOW_GAIN, LOW_GAIN);
			EXTEfx.alEffectf(equalizer, EXTEfx.AL_EQUALIZER_MID1_GAIN, 1);
			EXTEfx.alEffectf(equalizer, EXTEfx.AL_EQUALIZER_MID2_CENTER, MID_CENTER);
			EXTEfx.alEffectf(equalizer, EXTEfx.AL_EQUALIZER_MID2_WIDTH, 1);
			EXTEfx.alEffectf(equalizer, EXTEfx.AL_EQUALIZER_MID2_GAIN, MID_GAIN);
			EXTEfx.alEffectf(equalizer, EXTEfx.AL_EQUALIZER_HIGH_CUTOFF, HIGH_CUTOFF);
			EXTEfx.alEffectf(equalizer, EXTEfx.AL_EQUALIZER_HIGH_GAIN, HIGH_GAIN);
			EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, equalizer);
		}
		AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, now > 0 ? slot : EXTEfx.AL_EFFECTSLOT_NULL, 0, sendFilter);
	}

	/** On the second send, which a device may not have: a short, dark ring off the walls, its slot's gain following the ceiling. */
	private static void reverberated(int source, float now) {
		long device = ALC10.alcGetContextsDevice(ALC10.alcGetCurrentContext());
		if (ALC10.alcGetInteger(device, EXTEfx.ALC_MAX_AUXILIARY_SENDS) < 2) {
			return;
		}
		if (reverbSlot == 0 || !EXTEfx.alIsAuxiliaryEffectSlot(reverbSlot)) {
			reverbSlot = EXTEfx.alGenAuxiliaryEffectSlots();
			reverb = EXTEfx.alGenEffects();
			EXTEfx.alEffecti(reverb, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DENSITY, 1);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DIFFUSION, 0.8F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_GAIN, 0.32F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_GAINHF, 0.05F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DECAY_TIME, 0.6F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DECAY_HFRATIO, 0.2F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, 0.3F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_REFLECTIONS_DELAY, 0.005F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, 1);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_LATE_REVERB_DELAY, 0.01F);
			EXTEfx.alAuxiliaryEffectSloti(reverbSlot, EXTEfx.AL_EFFECTSLOT_EFFECT, reverb);
		}
		float ring = now * enclosed * REVERB;
		EXTEfx.alAuxiliaryEffectSlotf(reverbSlot, EXTEfx.AL_EFFECTSLOT_GAIN, ring);
		AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, ring > 0 ? reverbSlot : EXTEfx.AL_EFFECTSLOT_NULL, 1, sendFilter);
	}

	/** On the sound thread, for the tests: how much of the highs the path heard most lets through, or 1 on a device without EFX. */
	public static float filterHighs() {
		int heard = strength > 0.5F ? sendFilter : filter;
		return AL.getCapabilities().ALC_EXT_EFX && heard != 0 ? EXTEfx.alGetFilterf(heard, EXTEfx.AL_LOWPASS_GAINHF) : 1;
	}
}
