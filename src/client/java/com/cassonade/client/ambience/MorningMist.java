package com.cassonade.client.ambience;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;

/**
 * Now and then, about one morning in eight, the land wakes in a light mist, lifting as the sun climbs: the game's fog drawn closer, paler,
 * hiding the sky, thinning with height and gone indoors (AtmosphericFogEnvironmentMixin, SkyRendererMixin). Never thick enough to hide the way.
 */
public final class MorningMist {
	private static final int ONE_MORNING_IN = 8;
	private static final int RISES_FROM = 22000;
	private static final int FULL_FROM = 23500;
	private static final int FULL_UNTIL = 500;
	private static final int GONE_AT = 3000;
	private static final int FADES_ABOVE = 120;
	private static final int GONE_ABOVE = 180;
	private static final int AREA_STEP = 5;
	private static final float EASE = 0.01F;
	private static final float START = 6;
	private static final float END = 80;
	/** The height of the game's sky disc above the camera: at this end the fog covers it all, overhead too, leaving no silhouette. */
	private static final float SKY_END = 16;
	/** Below the height of the clouds, which are gone before the sky is. */
	private static final float CLOUD_END = 64;
	/** The fog's brightest channel from which the mist is at its palest: by night it stays as dark as the night. */
	private static final float DAYLIGHT = 0.6F;
	private static final float PALE = 0.55F;
	private static float density;

	private MorningMist() {
	}

	static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null) {
			density = 0;
			return;
		}
		if (!minecraft.isPaused()) {
			density = Mth.approach(density, target(level, minecraft.player.blockPosition()), EASE);
		}
	}

	/** Drawn from the morning's number rather than at random, so a morning keeps its answer from one tick to the next. */
	public static boolean comes(long morning) {
		return Math.floorMod(Mth.murmurHash3Mixer((int) morning + 1), ONE_MORNING_IN) == 0;
	}

	private static float target(ClientLevel level, BlockPos pos) {
		if (!Ambience.overworld(level) || level.isRaining()) {
			return 0;
		}
		// Counted from the evening before, so a morning is one whole, across the clock's turn at 24000
		long morning = Math.floorDiv(level.getDefaultClockTime() + Ambience.TICKS_PER_DAY - RISES_FROM, Ambience.TICKS_PER_DAY);
		if (!comes(morning)) {
			return 0;
		}
		float height = 1 - Mth.clamp((pos.getY() - FADES_ABOVE) / (float) (GONE_ABOVE - FADES_ABOVE), 0, 1);
		return ofTime(Ambience.tickOfDay(level)) * height * outdoors(level, pos);
	}

	/**
	 * Whether most of the area around has the sky above it. Not the light at the player's own block, which jumps under each tree.
	 * All or nothing, and blind to what is solid (dark inside, whatever is above it), so walking along a slope or past a house
	 * leaves the mist as it is.
	 */
	private static float outdoors(ClientLevel level, BlockPos pos) {
		int open = 0;
		int closed = 0;
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				BlockPos sample = pos.offset(x * AREA_STEP, 1, z * AREA_STEP);
				if (level.getBrightness(LightLayer.SKY, sample) > 0) {
					open++;
				} else if (!level.getBlockState(sample).isSolidRender()) {
					closed++;
				}
			}
		}
		return open > closed ? 1 : 0;
	}

	private static float ofTime(long tick) {
		if (tick >= RISES_FROM) {
			return Mth.clamp((tick - RISES_FROM) / (float) (FULL_FROM - RISES_FROM), 0, 1);
		}
		return tick <= FULL_UNTIL ? 1 : Mth.clamp(1 - (tick - FULL_UNTIL) / (float) (GONE_AT - FULL_UNTIL), 0, 1);
	}

	public static float density() {
		return density;
	}

	public static float start(float vanilla) {
		return Mth.lerp(density, vanilla, Math.min(START, vanilla));
	}

	public static float end(float vanilla) {
		return eased(vanilla, END);
	}

	public static float skyEnd(float vanilla) {
		return eased(vanilla, SKY_END);
	}

	public static float cloudEnd(float vanilla) {
		return eased(vanilla, CLOUD_END);
	}

	/** Geometric: from the game's distant end, a straight line would double the distance seen at the first tenth of density lost. */
	private static float eased(float vanilla, float misty) {
		return (float) Math.exp(Mth.lerp(density, Math.log(vanilla), Math.log(Math.min(misty, vanilla))));
	}

	public static Vector3fc color(Vector3fc color) {
		float light = Mth.clamp(Math.max(color.x(), Math.max(color.y(), color.z())) / DAYLIGHT, 0, 1);
		return ARGB.srgbLerp(density * PALE, color, new Vector3f(0.85F * light, 0.87F * light, 0.88F * light));
	}

	/** What is left of what the sky holds (sun, moon, stars), hidden behind the mist. */
	public static float veil() {
		return 1 - density;
	}
}
