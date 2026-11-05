package com.cassonade.time;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Full moon nights are peaceful: no monster spawns on the surface, under the open sky or the trees, while caves stay as
 * dark and dangerous as ever. The moon's own light (data/cassonade/timeline/moonlight.json) makes those nights the
 * brightest, and new moon nights the darkest; spawning follows only the full moon.
 */
public final class FullMoon {
	private FullMoon() {
	}

	/** Whether a monster may not spawn here tonight. Slimes keep their swamp nights, at their most under a full moon. */
	public static boolean peaceful(ServerLevel level, EntityType<?> type, BlockPos pos) {
		// Only where a moon rises: elsewhere the phase reads full by default
		if (type.getCategory() != MobCategory.MONSTER || type == EntityTypes.SLIME || !level.dimensionType().hasSkyLight()
				|| level.dimensionType().hasCeiling()) {
			return false;
		}
		return level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos) == MoonPhase.FULL_MOON
				&& pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
	}
}
