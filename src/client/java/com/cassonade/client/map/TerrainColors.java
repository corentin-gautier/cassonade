package com.cassonade.client.map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Top-down colors of the loaded terrain, shaded like a vanilla map. Colors are packed like a map
 * item's: {@link MapColor#getPackedId}, one byte per block, 0 for nothing.
 */
public final class TerrainColors {
	/** No ceiling: colors come from the surface, as on the overworld. */
	public static final int NO_CEILING = Integer.MAX_VALUE;

	private static final int MAX_WATER_DEPTH = 10;
	private static final int MAX_CAVE_DEPTH = 48;
	private static final int BIOME_STEP = 4;

	private TerrainColors() {
	}

	/**
	 * Fills {@code out} with the packed colors of a {@code size} × {@code size} square whose north-west
	 * corner is ({@code minX}, {@code minZ}), row by row. Columns in unloaded chunks are left at 0.
	 * Below a {@code ceiling} (the Nether roof), each column shows the first floor under that height.
	 */
	public static void sample(Level level, int minX, int minZ, int size, int ceiling, byte[] out) {
		sample(level, minX, minZ, size, size, ceiling, out, 0, size);
	}

	/** The same for a {@code width} × {@code height} rectangle, written from {@code offset} in rows of {@code stride}. */
	public static void sample(Level level, int minX, int minZ, int width, int height, int ceiling, byte[] out, int offset, int stride) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		// Heights of the row north of the one being sampled, for the slope shading
		int[] northHeights = new int[width];
		for (int x = 0; x < width; x++) {
			northHeights[x] = floor(level, chunk(level, minX + x, minZ - 1), minX + x, minZ - 1, ceiling, pos);
		}

		for (int z = 0; z < height; z++) {
			for (int x = 0; x < width; x++) {
				int worldX = minX + x;
				int worldZ = minZ + z;
				// Once per column: going through the level would look it up for each block read
				LevelChunk chunk = chunk(level, worldX, worldZ);
				int y = floor(level, chunk, worldX, worldZ, ceiling, pos);
				out[offset + x + z * stride] = y == Integer.MIN_VALUE ? 0 : color(level, chunk, worldX, worldZ, y, northHeights[x], pos);
				northHeights[x] = y;
			}
		}
	}

	private static @Nullable LevelChunk chunk(Level level, int x, int z) {
		return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
	}

	/** Samples the surface biome at the centre of each 4 × 4 block cell in the chunk. */
	public static void sampleBiomes(Level level, int minX, int minZ, int size, int ceiling, Identifier[] out) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int samplesPerRow = size / BIOME_STEP;
		for (int z = 0; z < size; z += BIOME_STEP) {
			for (int x = 0; x < size; x += BIOME_STEP) {
				int worldX = minX + x + BIOME_STEP / 2;
				int worldZ = minZ + z + BIOME_STEP / 2;
				int height = floor(level, chunk(level, worldX, worldZ), worldX, worldZ, ceiling, pos);
				out[x / BIOME_STEP + z / BIOME_STEP * samplesPerRow] = height == Integer.MIN_VALUE ? null
						: level.getBiome(pos.set(worldX, height, worldZ)).unwrapKey().map(key -> key.identifier()).orElse(null);
			}
		}
	}

	/** Y of the block seen from above, or {@link Integer#MIN_VALUE} when there is none: chunk not loaded or still empty. */
	private static int floor(Level level, @Nullable LevelChunk chunk, int x, int z, int ceiling, BlockPos.MutableBlockPos pos) {
		if (chunk == null) {
			return Integer.MIN_VALUE;
		}

		int y = Math.min(chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15), ceiling);
		if (ceiling != NO_CEILING && !chunk.getBlockState(pos.set(x, y, z)).isAir()) {
			// Inside a wall at the player's height: show the wall itself
			return y;
		}
		for (int depth = 0; depth < MAX_CAVE_DEPTH && y >= level.getMinY(); depth++, y--) {
			if (chunk.getBlockState(pos.set(x, y, z)).getMapColor(level, pos) != MapColor.NONE) {
				return y;
			}
		}
		return Integer.MIN_VALUE;
	}

	private static byte color(Level level, LevelChunk chunk, int x, int z, int y, int northY, BlockPos.MutableBlockPos pos) {
		BlockState state = chunk.getBlockState(pos.set(x, y, z));
		MapColor mapColor = state.getMapColor(level, pos);
		if (mapColor == MapColor.NONE) {
			return 0;
		}

		// Same formulas as MapItem: water darkens with depth, land is lit on north-facing slopes
		double shade;
		MapColor.Brightness brightness;
		if (state.getFluidState().is(FluidTags.WATER)) {
			int depth = 1;
			while (depth < MAX_WATER_DEPTH && chunk.getBlockState(pos.set(x, y - depth, z)).getFluidState().is(FluidTags.WATER)) {
				depth++;
			}
			shade = depth * 0.1 + ((x + z) & 1) * 0.2;
			brightness = shade < 0.5 ? MapColor.Brightness.HIGH : shade > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
		} else {
			shade = northY == Integer.MIN_VALUE ? 0 : (y - northY) * 0.8 + (((x + z) & 1) - 0.5) * 0.4;
			brightness = shade > 0.6 ? MapColor.Brightness.HIGH : shade < -0.6 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
		}
		return mapColor.getPackedId(brightness);
	}
}
