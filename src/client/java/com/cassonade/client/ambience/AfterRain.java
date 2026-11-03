package com.cassonade.client.ambience;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Once the rain stops, the trees keep dripping for a minute, less and less (LeavesBlockDripMixin): every leaf block
 * the rain could reach, not only the canopy's top layer, which is all vanilla wets while it rains.
 */
public final class AfterRain {
	/** How long the leaves drip once the rain has stopped. */
	private static final int DRIPPING_TICKS = 1200;
	/** Ticks since the rain stopped, as long as it has not rained in this level yet. */
	private static int dry = DRIPPING_TICKS;
	private static ClientLevel lastLevel;

	private AfterRain() {
	}

	static void tick(ClientLevel level) {
		if (level != lastLevel) {
			lastLevel = level;
			dry = DRIPPING_TICKS;
		}
		if (level.isRaining()) {
			dry = 0;
		} else if (dry < DRIPPING_TICKS) {
			dry++;
		}
	}

	/** From 1 as the rain stops to 0 a minute later, eased so the last drops are rare; 0 while it rains, vanilla dripping then. */
	public static float wetness(ClientLevel level) {
		if (level != lastLevel || level.isRaining() || !Ambience.overworld(level)) {
			return 0;
		}
		float left = 1 - (float) dry / DRIPPING_TICKS;
		return left * left;
	}

	/** Whether the rain fell on these leaves: nothing but leaves above them, in a biome where it rains rather than snows. */
	public static boolean wasRainedOn(ClientLevel level, BlockPos pos) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) <= pos.getY()
				&& level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel()) == Biome.Precipitation.RAIN;
	}
}
