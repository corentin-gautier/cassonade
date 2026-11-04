package com.cassonade.tweaks;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import com.cassonade.Cassonade;

/**
 * The towns, rare villages grown larger and denser: all data (worldgen/structure/town_*, their pools written by
 * tools/town_pools.py), in the villages' structure set and tag so the rest of what villages get applies to them.
 */
public final class Towns {
	/** Every biome's town. */
	public static final TagKey<Structure> TAG = TagKey.create(Registries.STRUCTURE, Cassonade.id("town"));

	private Towns() {
	}
}
