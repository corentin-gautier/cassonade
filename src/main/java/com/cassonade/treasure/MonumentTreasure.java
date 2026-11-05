package com.cassonade.treasure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;

import com.cassonade.Cassonade;

/**
 * The ocean monument's treasure: vanilla only hides gold blocks in its treasure room, past the elder guardians and
 * deep under murky water, so a few barrels of rare loot sit in its corners (`OceanMonumentCoreRoomMixin`). Vanilla
 * barrels with a loot table, placed as the world generates, so a world opened without the mod keeps them.
 */
public final class MonumentTreasure {
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Cassonade.id("chests/ocean_monument"));
	/** One corner in this many stays empty: 3 barrels on average, never the same layout. */
	private static final int EMPTY_CORNER = 4;

	private MonumentTreasure() {
	}

	public static boolean holdsBarrel(BlockPos pos) {
		return RandomSource.create(pos.asLong() ^ 0x6d6f6eL).nextInt(EMPTY_CORNER) != 0;
	}
}
