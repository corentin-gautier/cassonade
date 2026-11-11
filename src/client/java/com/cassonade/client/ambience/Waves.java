package com.cassonade.client.ambience;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;

import com.cassonade.Cassonade;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * The sea, heard all around as it nears: two unbroken stereo loops without a position, a dull roar from afar and clear waves at
 * the water's edge, each eased to a volume that follows the distance to the nearest shore. Only where the sea meets low ground:
 * no surf against a cliff, under ice or in a lake. In open water, far from anything to break on, the roar alone stays, quieter:
 * the swell all around.
 */
public final class Waves {
	public static final Identifier NEAR = Cassonade.id("waves");
	public static final Identifier FAR = Cassonade.id("waves_far");
	private static final int REACH = 48;
	private static final int STEP = 4;
	/** How much higher than the water the ground may be for waves to break on it: above, it is a cliff. */
	static final int MAX_BANK = 3;
	/** Under this distance from the shore, the clear waves come in, at their fullest at the very edge. */
	private static final float NEAR_REACH = 18;
	/** The last ring of the scan straight ahead, so that the roar has died out by the time a shore goes out of sight. */
	private static final float FAR_REACH = REACH - STEP;
	private static final float NEAR_VOLUME = 0.2F;
	private static final float FAR_VOLUME = 0.17F;
	/** The swell out at sea, under the roar of a shore close by, so that nearing land still sounds like it. */
	private static final float OPEN_SEA_VOLUME = 0.08F;
	/** Up to this height above the sea's surface waves are heard in full: from a boat or flying low. */
	private static final int FULL_HEIGHT = 8;
	/** Above it they fade over this many more blocks, as when climbing away from the shore, and are gone from up in the sky. */
	private static final int FADE_HEIGHT = 16;
	/** Volume per tick: a few seconds to come or go, unnoticed while walking. */
	private static final float EASE = 0.004F;
	private static final int SCAN_EVERY = 40;
	/**
	 * The length of the clear waves' loop, and a moment before each of its loudest waves, in ticks from its start. At least the
	 * foam's run apart (Foam.LIFE), across the loop's end too: the next surge clears the foam before it has run back.
	 */
	private static final int LOOP_TICKS = 105 * 20;
	private static final int[] SURGES = {65, 165, 505, 631, 754, 935, 1048, 1330, 1630, 1750, 1985, 2092};
	private static final AmbientLoop NEAR_LOOP = new AmbientLoop(NEAR, EASE);
	private static final AmbientLoop FAR_LOOP = new AmbientLoop(FAR, EASE);
	private static int nextScan;
	private static int loopTick;
	private static float nearTarget;
	private static float farTarget;
	private static boolean forcedNear;
	private static boolean forcedFar;
	private static boolean surging = true;

	private Waves() {
	}

	static void tick(Minecraft minecraft, ClientLevel level) {
		if (--nextScan <= 0) {
			nextScan = SCAN_EVERY;
			scan(minecraft, level);
		}
		// Off, the sound fades out as it would walking inland
		boolean on = Settings.on(Setting.WAVES);
		NEAR_LOOP.follow(minecraft, forcedNear ? NEAR_VOLUME : on ? nearTarget : 0);
		FAR_LOOP.follow(minecraft, forcedFar ? FAR_VOLUME : on ? farTarget : 0);
		Foam.tick();
		// The sound plays on while the game is paused or not, but this only ticks along with it unpaused: close enough for a wave
		if (NEAR_LOOP.playing()) {
			loopTick = (loopTick + 1) % LOOP_TICKS;
			for (int surge : SURGES) {
				if (loopTick == surge && surging) {
					Foam.surge(minecraft, level);
				}
			}
		} else {
			loopTick = 0;
		}
	}

	private static void scan(Minecraft minecraft, ClientLevel level) {
		nearTarget = 0;
		farTarget = 0;
		BlockPos pos = minecraft.player.blockPosition();
		if (!Ambience.overworld(level) || level.getBrightness(LightLayer.SKY, pos) == 0 || !AirSounds.nearCoast(level, pos)) {
			return;
		}
		BlockPos shore = shore(level, pos);
		if (shore != null) {
			float distance = Mth.sqrt((float) shore.distToCenterSqr(pos.getX() + 0.5, shore.getY() + 0.5, pos.getZ() + 0.5));
			float height = altitude(pos.getY() - shore.getY());
			nearTarget = NEAR_VOLUME * AmbientSound.closeness(distance, NEAR_REACH) * height;
			farTarget = FAR_VOLUME * AmbientSound.closeness(distance, FAR_REACH) * height;
		}
		int above = openSeaHeight(level, pos);
		if (above != Integer.MIN_VALUE) {
			farTarget = Math.max(farTarget, OPEN_SEA_VOLUME * altitude(above));
		}
	}

	/** 1 up to FULL_HEIGHT above the water, easing down to 0 over FADE_HEIGHT more blocks. */
	private static float altitude(int above) {
		return AmbientSound.closeness(Math.max(0, above - FULL_HEIGHT), FADE_HEIGHT);
	}

	/** Height above the sea's surface where it is open sea, else MIN_VALUE; under ice, MOTION_BLOCKING stops at the ice, which is no water. */
	private static int openSeaHeight(ClientLevel level, BlockPos pos) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) - 1;
		BlockPos surface = new BlockPos(pos.getX(), top, pos.getZ());
		int above = pos.getY() - top;
		boolean sea = above >= -1 && level.getFluidState(surface).is(FluidTags.WATER) && level.getBiome(surface).is(BiomeTags.IS_OCEAN);
		return sea ? above : Integer.MIN_VALUE;
	}

	/** The nearest place around where the sea's surface meets low ground, if any. */
	public static @Nullable BlockPos shore(ClientLevel level, BlockPos pos) {
		int size = 2 * REACH / STEP + 1;
		int[] tops = new int[size * size];
		boolean[] sea = new boolean[size * size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				int x = pos.getX() - REACH + i * STEP;
				int z = pos.getZ() - REACH + j * STEP;
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
				BlockPos surface = new BlockPos(x, top, z);
				tops[i * size + j] = top;
				sea[i * size + j] = level.getFluidState(surface).is(FluidTags.WATER) && AirSounds.coast(level, surface);
			}
		}
		BlockPos nearest = null;
		long closest = Long.MAX_VALUE;
		for (int i = 1; i < size - 1; i++) {
			for (int j = 1; j < size - 1; j++) {
				int here = i * size + j;
				if (sea[here] && (bank(tops, sea, here, here - size) || bank(tops, sea, here, here + size) || bank(tops, sea, here, here - 1) || bank(tops, sea, here, here + 1))) {
					BlockPos candidate = new BlockPos(pos.getX() - REACH + i * STEP, tops[here], pos.getZ() - REACH + j * STEP);
					long distance = (long) candidate.distToCenterSqr(pos.getX() + 0.5, candidate.getY() + 0.5, pos.getZ() + 0.5);
					if (distance < closest) {
						closest = distance;
						nearest = candidate;
					}
				}
			}
		}
		return nearest;
	}

	/** Whether the column beside is dry ground, little above the water; water higher or lower than the sea is no bank either. */
	private static boolean bank(int[] tops, boolean[] sea, int water, int beside) {
		int rise = tops[beside] - tops[water];
		return !sea[beside] && rise >= 0 && rise <= MAX_BANK;
	}

	/** For the development command: on or off, wherever the player is. */
	static void toggleNear() {
		forcedNear = !forcedNear;
	}

	static void toggleFar() {
		forcedFar = !forcedFar;
	}

	/** For tests: the foam only comes when asked, not with the sound's waves. */
	public static void setSurging(boolean on) {
		surging = on;
	}

	/** For tests: looks for the shore again without waiting its turn. */
	public static void scanNext() {
		nextScan = 0;
	}

	public static float nearVolume() {
		return NEAR_LOOP.shown();
	}

	public static float farVolume() {
		return FAR_LOOP.shown();
	}
}
