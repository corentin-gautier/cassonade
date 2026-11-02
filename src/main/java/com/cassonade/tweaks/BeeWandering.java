package com.cassonade.tweaks;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps wandering bees from drifting out to sea, where nothing brings them back: they turn back at the shore, and
 * one already out there heads for the nearest land. Ponds and rivers are not the sea, and a bee far from its hive
 * still crosses the water to get home.
 */
public final class BeeWandering {
	/** Vanilla's distance from the hive beyond which a wandering bee heads back to it. */
	private static final int HOMING_DISTANCE = 24;
	private static final int SEARCH_STEP = 8;
	private static final int SEARCH_RANGE = 64;
	private static final int SEARCH_DIRECTIONS = 8;

	private BeeWandering() {
	}

	/** Where a bee wanders given the place vanilla picked, null to stay where it is for now. */
	public static @Nullable Vec3 destination(Bee bee, @Nullable Vec3 picked) {
		BlockPos hive = bee.getHivePos();
		if (hive != null && !hive.closerThan(bee.blockPosition(), HOMING_DISTANCE)
				|| picked != null && !isOverSea(bee.level(), BlockPos.containing(picked))) {
			return picked;
		}
		boolean atSea = isOverSea(bee.level(), bee.blockPosition());
		Vec3 land = towardsLand(bee);
		if (land == null) {
			return atSea ? picked : null;
		}
		Vec3 target = HoverRandomPos.getPos(bee, 8, 7, land.x, land.z, (float) (Math.PI / 2), 3, 1);
		if (target == null) {
			target = AirAndWaterRandomPos.getPos(bee, 8, 4, -2, land.x, land.z, (float) (Math.PI / 2));
		}
		// Out at sea any step towards the land will do; from the shore, one over the water is what is avoided
		return target != null && !atSea && isOverSea(bee.level(), BlockPos.containing(target)) ? null : target;
	}

	/** The direction of the nearest land in loaded chunks, null if there is none. */
	private static @Nullable Vec3 towardsLand(Bee bee) {
		BlockPos pos = bee.blockPosition();
		for (int distance = SEARCH_STEP; distance <= SEARCH_RANGE; distance += SEARCH_STEP) {
			for (int i = 0; i < SEARCH_DIRECTIONS; i++) {
				double angle = i * 2 * Math.PI / SEARCH_DIRECTIONS;
				double x = Math.cos(angle);
				double z = Math.sin(angle);
				BlockPos column = pos.offset((int) Math.round(x * distance), 0, (int) Math.round(z * distance));
				if (bee.level().isLoaded(column) && !isOverSea(bee.level(), column)) {
					return new Vec3(x, 0, z);
				}
			}
		}
		return null;
	}

	/** Open water on top of the column in an ocean biome. Unloaded columns don't count, nothing can be told of them. */
	public static boolean isOverSea(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return false;
		}
		BlockPos top = new BlockPos(pos.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) - 1, pos.getZ());
		return level.getFluidState(top).is(FluidTags.WATER) && level.getBiome(top).is(BiomeTags.IS_OCEAN);
	}
}
