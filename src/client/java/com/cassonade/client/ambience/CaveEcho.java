package com.cassonade.client.ambience;

import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * In a cave, the rock sends every sound back: a reverb on the world's sounds, on the first send, which only the water uses
 * otherwise (UnderwaterSound). A passage rings a little and briefly, a great hall loud and long; the water has its own echo.
 */
public final class CaveEcho {
	/** Ticks between two measures of the cave, and the ease from a passage to a hall, slow as one walks in. */
	private static final int LOOK_EVERY = 20;
	private static final float EASE = 0.02F;
	/** Out of the sky's light and this far under the surface, a little light from a cave's mouth allowed. */
	private static final int CAVE_LIGHT = 3;
	private static final int CAVE_DEPTH = 6;
	/** How far each look reaches, and the mean free distance of a passage and of a great hall. */
	private static final int REACH = 32;
	private static final float NARROW = 3;
	private static final float WIDE = 12;
	/** How loud the echo of a passage is, against a hall's; and how long each rings, in seconds. */
	private static final float PASSAGE = 0.2F;
	private static final float SHORT_DECAY = 1.2F;
	private static final float LONG_DECAY = 3;
	/** The decay is set again only past this change, the effect then attached to its slot anew. */
	private static final float DECAY_STEP = 0.2F;
	/**
	 * Around and above the eyes, not below: the floor is always close under the feet, and would make a hall as dull as a
	 * passage.
	 */
	private static final Vec3[] LOOKS = looks();
	private static volatile float echo;
	/** From a passage, 0, to a great hall, 1. */
	private static volatile float size;
	private static float target;
	private static float targetSize;
	private static float decay;
	private static int nextLook;
	/** Made on first use; the context, and the slot with it, goes when the sound engine reloads. */
	private static int slot;
	private static int reverb;

	private CaveEcho() {
	}

	/** Eight bearings level with the eyes, four slanting up, and straight up. */
	private static Vec3[] looks() {
		Vec3[] looks = new Vec3[13];
		for (int i = 0; i < 8; i++) {
			float angle = i * Mth.TWO_PI / 8;
			looks[i] = new Vec3(Mth.cos(angle), 0, Mth.sin(angle));
		}
		for (int i = 0; i < 4; i++) {
			float angle = (i + 0.5F) * Mth.TWO_PI / 4;
			looks[8 + i] = new Vec3(Mth.cos(angle), 1, Mth.sin(angle)).normalize();
		}
		looks[12] = new Vec3(0, 1, 0);
		return looks;
	}

	/** On the render thread, from the sound engine's tick. Tells whether the sources playing must follow. */
	static boolean tick(Minecraft minecraft) {
		if (--nextLook <= 0) {
			nextLook = LOOK_EVERY;
			targetSize = measure(minecraft);
			target = targetSize < 0 ? 0 : Mth.lerp(targetSize, PASSAGE, 1);
		}
		float next = Mth.approach(echo, target, EASE);
		size = Mth.approach(size, Math.max(targetSize, 0), EASE);
		boolean changed = next != echo;
		echo = next;
		return changed;
	}

	/** -1 out of a cave, then from a passage, 0, to a hall, 1, by how far the rock is around, on average. */
	private static float measure(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		Camera camera = minecraft.gameRenderer.mainCamera();
		if (level == null || !level.dimensionType().hasSkyLight() || camera.getFluidInCamera() != FogType.NONE) {
			return -1;
		}
		Vec3 eye = camera.position();
		BlockPos at = BlockPos.containing(eye);
		if (level.getBrightness(LightLayer.SKY, at) > CAVE_LIGHT || level.getHeight(Heightmap.Types.WORLD_SURFACE, at.getX(), at.getZ()) - at.getY() < CAVE_DEPTH) {
			return -1;
		}
		float free = 0;
		for (Vec3 look : LOOKS) {
			HitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(REACH)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
			free += hit.getType() == HitResult.Type.MISS ? REACH : (float) hit.getLocation().distanceTo(eye);
		}
		return Mth.clamp((free / LOOKS.length - NARROW) / (WIDE - NARROW), 0, 1);
	}

	public static float echo() {
		return echo;
	}

	/** From a passage, 0, to a great hall, 1. */
	public static float size() {
		return size;
	}

	/** Whether the slot was made, so that a source may still be sent to it. */
	static boolean made() {
		return slot != 0;
	}

	/** On the sound thread: an airy ring off the rock, its slot's gain and its length following the cave's size. */
	static void send(int source) {
		float now = echo;
		if (now > 0 && (slot == 0 || !EXTEfx.alIsAuxiliaryEffectSlot(slot))) {
			slot = EXTEfx.alGenAuxiliaryEffectSlots();
			reverb = EXTEfx.alGenEffects();
			EXTEfx.alEffecti(reverb, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DENSITY, 1);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DIFFUSION, 1);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_GAIN, 0.45F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_GAINHF, 0.5F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DECAY_HFRATIO, 0.6F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, 0.8F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_REFLECTIONS_DELAY, 0.03F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, 1.6F);
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_LATE_REVERB_DELAY, 0.04F);
			decay = 0;
		}
		// A hall rings longer: the slot copies the effect when attached, so it is attached again once changed enough
		float wanted = Mth.lerp(size, SHORT_DECAY, LONG_DECAY);
		if (slot != 0 && Math.abs(wanted - decay) > DECAY_STEP) {
			decay = wanted;
			EXTEfx.alEffectf(reverb, EXTEfx.AL_REVERB_DECAY_TIME, decay);
			EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, reverb);
		}
		if (slot != 0) {
			EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, now);
			AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, now > 0 ? slot : EXTEfx.AL_EFFECTSLOT_NULL, 0, EXTEfx.AL_FILTER_NULL);
		}
	}
}
