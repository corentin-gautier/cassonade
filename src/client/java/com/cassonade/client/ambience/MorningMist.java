package com.cassonade.client.ambience;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;

/**
 * One morning out of two, the land wakes in a light mist, lifting as the sun climbs: the game's fog drawn closer, paler,
 * thinning with height and gone indoors (AtmosphericFogEnvironmentMixin). Never thick enough to hide the way.
 */
public final class MorningMist {
	private static final int RISES_FROM = 22000;
	private static final int FULL_FROM = 23500;
	private static final int FULL_UNTIL = 500;
	private static final int GONE_AT = 3000;
	private static final int FADES_ABOVE = 90;
	private static final int GONE_ABOVE = 150;
	private static final int AREA_STEP = 5;
	private static final float EASE = 0.01F;
	private static final float START = 6;
	private static final float END = 80;
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

	private static float target(ClientLevel level, BlockPos pos) {
		if (!Ambience.overworld(level) || level.isRaining()) {
			return 0;
		}
		// Counted from the evening before, so a morning is one whole, across the clock's turn at 24000
		long morning = Math.floorDiv(level.getDefaultClockTime() + Ambience.TICKS_PER_DAY - RISES_FROM, Ambience.TICKS_PER_DAY);
		if (morning % 2 != 0) {
			return 0;
		}
		float height = 1 - Mth.clamp((pos.getY() - FADES_ABOVE) / (float) (GONE_ABOVE - FADES_ABOVE), 0, 1);
		return ofTime(Ambience.tickOfDay(level)) * height * outdoors(level, pos);
	}

	/**
	 * How much of the area around has the sky above it, from 0 in a cave to 1 outside. Not the light at the player's own
	 * block, which jumps under each tree and would make the fog flicker while walking through a wood.
	 */
	private static float outdoors(ClientLevel level, BlockPos pos) {
		int open = 0;
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				if (level.getBrightness(LightLayer.SKY, pos.offset(x * AREA_STEP, 1, z * AREA_STEP)) > 0) {
					open++;
				}
			}
		}
		return open / 9F;
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
		return Mth.lerp(density, vanilla, Math.min(END, vanilla));
	}

	public static float pale() {
		return density * PALE;
	}
}
