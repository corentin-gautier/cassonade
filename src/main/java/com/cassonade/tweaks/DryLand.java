package com.cassonade.tweaks;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import com.cassonade.Cassonade;

/**
 * The mod's buildings that stand on the ground (the lighthouse, the lone dwellings but the fisherman's shack): a
 * jigsaw piece goes on the first free block over its chunk's middle, which over the sea is the water's surface, and
 * terrain adaptation only fills a few blocks under it, so on a beach biome reaching out into the water a lighthouse
 * floated over the sea. Once its pieces are laid out, the ground under all of them, from the terrain's heights alone,
 * must be dry or under shallow water that the terrain adaptation fills; otherwise it is not built there.
 */
public final class DryLand {
	public static final TagKey<Structure> TAG = TagKey.create(Registries.STRUCTURE, Cassonade.id("on_dry_land"));
	/** The most water allowed over the ground under the building. */
	private static final int SHALLOW = 2;
	/** The ground is read every this many blocks on each axis, and at the far edges. */
	private static final int STEP = 2;

	private DryLand() {
	}

	public static boolean under(Structure.GenerationContext context, BoundingBox box) {
		for (int x = box.minX(); x <= box.maxX() + STEP - 1; x += STEP) {
			for (int z = box.minZ(); z <= box.maxZ() + STEP - 1; z += STEP) {
				int cx = Math.min(x, box.maxX());
				int cz = Math.min(z, box.maxZ());
				int surface = height(context, cx, cz, Heightmap.Types.WORLD_SURFACE_WG);
				int ground = height(context, cx, cz, Heightmap.Types.OCEAN_FLOOR_WG);
				if (surface - ground > SHALLOW) {
					return false;
				}
			}
		}
		return true;
	}

	private static int height(Structure.GenerationContext context, int x, int z, Heightmap.Types type) {
		return context.chunkGenerator().getBaseHeight(x, z, type, context.heightAccessor(), context.randomState());
	}
}
