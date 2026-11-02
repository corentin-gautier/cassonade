package com.cassonade.client.ambience;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;

import com.cassonade.Cassonade;

/**
 * The sea, heard all around as it nears: two unbroken stereo loops without a position, a dull roar from afar and clear waves at
 * the water's edge, each eased to a volume that follows the distance to the nearest shore. Only where the sea meets low ground:
 * no surf against a cliff, under ice or in a lake, and none in open water, far from anything to break on.
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
	/** Volume per tick: a few seconds to come or go, unnoticed while walking. */
	private static final float EASE = 0.004F;
	private static final int SCAN_EVERY = 40;
	/** The length of the clear waves' loop, and a moment before each of its loudest waves, in ticks from its start. */
	private static final int LOOP_TICKS = 105 * 20;
	private static final int[] SURGES = {65, 220, 505, 935, 1330, 1630, 1750, 1985};
	private static final Loop NEAR_LOOP = new Loop(NEAR);
	private static final Loop FAR_LOOP = new Loop(FAR);
	private static int nextScan;
	private static int loopTick;
	private static float nearTarget;
	private static float farTarget;
	private static boolean forcedNear;
	private static boolean forcedFar;

	private Waves() {
	}

	static void tick(Minecraft minecraft, ClientLevel level) {
		if (--nextScan <= 0) {
			nextScan = SCAN_EVERY;
			scan(minecraft, level);
		}
		NEAR_LOOP.follow(minecraft, forcedNear ? NEAR_VOLUME : nearTarget);
		FAR_LOOP.follow(minecraft, forcedFar ? FAR_VOLUME : farTarget);
		Foam.tick(level);
		// The sound plays on while the game is paused or not, but this only ticks along with it unpaused: close enough for a wave
		if (NEAR_LOOP.playing != null) {
			loopTick = (loopTick + 1) % LOOP_TICKS;
			for (int surge : SURGES) {
				if (loopTick == surge) {
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
			nearTarget = NEAR_VOLUME * AmbientSound.closeness(distance, NEAR_REACH);
			farTarget = FAR_VOLUME * AmbientSound.closeness(distance, FAR_REACH);
		}
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

	/** For tests: looks for the shore again without waiting its turn. */
	public static void scanNext() {
		nextScan = 0;
	}

	public static float nearVolume() {
		return NEAR_LOOP.shown;
	}

	public static float farVolume() {
		return FAR_LOOP.shown;
	}

	/** Its volume kept here, for the game to read each frame; started once it has one, stopped once it is silent. */
	private static final class Loop {
		final Identifier id;
		float shown;
		@Nullable Instance playing;

		Loop(Identifier id) {
			this.id = id;
		}

		void follow(Minecraft minecraft, float target) {
			shown = Mth.approach(shown, target, EASE);
			if (shown > 0 && playing == null) {
				playing = new Instance(this);
				minecraft.getSoundManager().play(playing);
			} else if (shown <= 0 && playing != null) {
				playing.end();
				playing = null;
			}
		}
	}

	private static final class Instance extends AbstractTickableSoundInstance {
		private final Loop loop;

		Instance(Loop loop) {
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
