package com.cassonade.client.map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import org.jspecify.annotations.Nullable;

import com.cassonade.Cassonade;

/**
 * Remembers every chunk explored around the player, whether a map is worn or not, so the world map
 * shows them all once one is. Kept on the client: in the world's save folder in singleplayer, in the
 * game folder per server address in multiplayer.
 */
public final class WorldMap {
	/** Chunks around the player that count as explored, about what the minimap shows. */
	private static final int RADIUS = 6;
	private static final int CHUNKS_PER_TICK = 8;
	private static final int SAVE_INTERVAL = 30 * 20;
	private static final int MAX_CACHED_REGIONS_PER_DIMENSION = 32;

	private static Path folder;
	private static final Map<ResourceKey<Level>, Dimension> dimensions = new HashMap<>();
	private static final ArrayDeque<long[]> queue = new ArrayDeque<>();
	private static final byte[] chunkColors = new byte[16 * 16];
	private static final Identifier[] chunkBiomes = new Identifier[4 * 4];
	private static int ticksSinceSave;

	private WorldMap() {
	}

	public static void open(Minecraft minecraft) {
		Path opening;
		if (minecraft.getSingleplayerServer() != null) {
			opening = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve("cassonade/worldmap");
		} else {
			ServerData server = minecraft.getCurrentServer();
			String name = server == null ? "unknown" : server.ip.replaceAll("[^A-Za-z0-9._-]", "_");
			opening = minecraft.gameDirectory.toPath().resolve("cassonade/worldmap/servers").resolve(name);
		}
		// Already opened by the loading screen, whose map must not go blank when the player joins
		if (opening.equals(folder)) {
			return;
		}
		close();
		folder = opening;
		DeathMarker.open(folder);
	}

	public static void close() {
		save();
		releaseTextures();
		dimensions.values().forEach(dimension -> dimension.overviews.abandon());
		dimensions.clear();
		queue.clear();
		folder = null;
		DeathMarker.close();
	}

	public static void tick(Minecraft minecraft) {
		if (folder == null || minecraft.level == null || minecraft.player == null) {
			return;
		}

		evictRegions(minecraft);
		Level level = minecraft.level;
		if (queue.isEmpty()) {
			queueChunksAround(SectionPos.blockToSectionCoord(minecraft.player.getBlockX()), SectionPos.blockToSectionCoord(minecraft.player.getBlockZ()));
		}
		int ceiling = level.dimensionType().hasCeiling() ? minecraft.player.getBlockY() + 1 : TerrainColors.NO_CEILING;
		for (int i = 0; i < CHUNKS_PER_TICK && !queue.isEmpty(); i++) {
			long[] chunk = queue.poll();
			int chunkX = (int) chunk[0];
			int chunkZ = (int) chunk[1];
			if (!level.hasChunk(chunkX, chunkZ)) {
				continue;
			}
			TerrainColors.sample(level, chunkX * 16, chunkZ * 16, 16, ceiling, chunkColors);
			TerrainColors.sampleBiomes(level, chunkX * 16, chunkZ * 16, 16, ceiling, chunkBiomes);
			// The client also holds chunks whose blocks have not arrived yet: they must not erase what was explored
			if (!isBlank(chunkColors)) {
				MapRegion region = region(level.dimension(), chunkX >> 5, chunkZ >> 5, true);
				if (region != null && region.setChunk(chunkX & 31, chunkZ & 31, chunkColors, chunkBiomes)) {
					dimensions.get(level.dimension()).overviews.markStale(chunkX >> 5, chunkZ >> 5);
				}
			}
		}

		if (++ticksSinceSave >= SAVE_INTERVAL) {
			save();
		}
	}

	private static boolean isBlank(byte[] colors) {
		for (byte color : colors) {
			if (color != 0) {
				return false;
			}
		}
		return true;
	}

	/** The saved surface biome under a block on the world map, or null if it is not known. */
	public static @Nullable Identifier biomeAt(ResourceKey<Level> dimensionKey, int blockX, int blockZ) {
		MapRegion region = region(dimensionKey, Math.floorDiv(blockX, MapRegion.BLOCKS), Math.floorDiv(blockZ, MapRegion.BLOCKS), false);
		return region == null ? null : region.biomeAt(Math.floorMod(blockX, MapRegion.BLOCKS), Math.floorMod(blockZ, MapRegion.BLOCKS));
	}

	/** Nearest chunks first, so a fast traveller still gets the chunks they pass through. */
	private static void queueChunksAround(int centerX, int centerZ) {
		List<long[]> chunks = new ArrayList<>();
		for (int dz = -RADIUS; dz <= RADIUS; dz++) {
			for (int dx = -RADIUS; dx <= RADIUS; dx++) {
				if (dx * dx + dz * dz <= RADIUS * RADIUS) {
					chunks.add(new long[] {centerX + dx, centerZ + dz});
				}
			}
		}
		chunks.sort(Comparator.comparingLong(chunk -> (chunk[0] - centerX) * (chunk[0] - centerX) + (chunk[1] - centerZ) * (chunk[1] - centerZ)));
		queue.addAll(chunks);
	}

	/** An explored region, loaded from disk when needed; null if nothing was explored there (and {@code create} is false). */
	public static MapRegion region(ResourceKey<Level> dimensionKey, int regionX, int regionZ, boolean create) {
		if (folder == null) {
			return null;
		}
		Dimension dimension = dimensions.computeIfAbsent(dimensionKey, Dimension::new);
		long key = (long) regionX << 32 | regionZ & 0xFFFFFFFFL;
		Optional<MapRegion> cached = dimension.regions.get(key);
		if (cached != null && (cached.isPresent() || !create)) {
			return cached.orElse(null);
		}

		Path file = dimension.folder().resolve("r." + regionX + "." + regionZ + ".bin");
		if (!create && !Files.exists(file)) {
			dimension.regions.put(key, Optional.empty());
			return null;
		}
		MapRegion region;
		try {
			region = MapRegion.load(file, dimension.textureId(regionX, regionZ));
		} catch (IOException e) {
			Cassonade.LOGGER.warn("Could not read world map region {}", file, e);
			region = new MapRegion(file, dimension.textureId(regionX, regionZ));
		}
		dimension.regions.put(key, Optional.of(region));
		return region;
	}

	/** Allows the overviews drawn next to make their share of textures; called once per frame. */
	public static void startOverviewFrame(ResourceKey<Level> dimensionKey) {
		if (folder != null) {
			dimensions.computeIfAbsent(dimensionKey, Dimension::new).overviews.startFrame();
		}
	}

	/** A region's low resolution texture, or null while it is being read or if nothing was explored there. */
	public static @Nullable Identifier overview(ResourceKey<Level> dimensionKey, int regionX, int regionZ) {
		if (folder == null) {
			return null;
		}
		Dimension dimension = dimensions.computeIfAbsent(dimensionKey, Dimension::new);
		long key = (long) regionX << 32 | regionZ & 0xFFFFFFFFL;
		Optional<MapRegion> cached = dimension.regions.get(key);
		return dimension.overviews.texture(regionX, regionZ, cached == null ? null : cached.orElse(null));
	}

	/** A region's low resolution texture in the tones of an old map, or null likewise. */
	public static @Nullable Identifier sepiaOverview(ResourceKey<Level> dimensionKey, int regionX, int regionZ) {
		if (folder == null) {
			return null;
		}
		Dimension dimension = dimensions.computeIfAbsent(dimensionKey, Dimension::new);
		long key = (long) regionX << 32 | regionZ & 0xFFFFFFFFL;
		Optional<MapRegion> cached = dimension.regions.get(key);
		return dimension.overviews.sepiaTexture(regionX, regionZ, cached == null ? null : cached.orElse(null));
	}

	/** Regions held in full in memory, for the tests. */
	public static long loadedRegions(ResourceKey<Level> dimensionKey) {
		Dimension dimension = dimensions.get(dimensionKey);
		return dimension == null ? 0 : dimension.regions.values().stream().filter(Optional::isPresent).count();
	}

	/** Overviews still being read, for the tests. */
	public static int pendingOverviews(ResourceKey<Level> dimensionKey) {
		Dimension dimension = dimensions.get(dimensionKey);
		return dimension == null ? 0 : dimension.overviews.pending();
	}

	/** The packed map color an overview holds for a block, 0 if it holds none, for the tests. */
	public static int overviewColor(ResourceKey<Level> dimensionKey, int blockX, int blockZ) {
		Dimension dimension = dimensions.get(dimensionKey);
		return dimension == null ? 0 : dimension.overviews.color(Math.floorDiv(blockX, MapRegion.BLOCKS), Math.floorDiv(blockZ, MapRegion.BLOCKS),
				Math.floorMod(blockX, MapRegion.BLOCKS) / 4, Math.floorMod(blockZ, MapRegion.BLOCKS) / 4);
	}

	/** Overviews showing explored land, for the tests. */
	public static int drawnOverviews(ResourceKey<Level> dimensionKey) {
		Dimension dimension = dimensions.get(dimensionKey);
		return dimension == null ? 0 : dimension.overviews.drawn();
	}

	/**
	 * Forgets the regions used longest ago. Only between frames and never under the world map screen: it
	 * can show more regions than the cache holds, and a texture released after it was queued for drawing
	 * has its id reused by another texture, which is then drawn in its place.
	 */
	private static void evictRegions(Minecraft minecraft) {
		if (minecraft.gui.screen() instanceof WorldMapScreen) {
			return;
		}
		for (Dimension dimension : dimensions.values()) {
			Iterator<Optional<MapRegion>> iterator = dimension.regions.values().iterator();
			while (dimension.regions.size() > MAX_CACHED_REGIONS_PER_DIMENSION) {
				MapRegion region = iterator.next().orElse(null);
				if (region != null) {
					try {
						region.save();
					} catch (IOException e) {
						Cassonade.LOGGER.warn("Could not save world map region while evicting it", e);
						break;
					}
					region.releaseTexture();
				}
				iterator.remove();
			}
		}
	}

	/** Frees the textures made for the world map screen. */
	public static void releaseTextures() {
		for (Dimension dimension : dimensions.values()) {
			dimension.overviews.releaseTextures();
			dimension.regions.values().forEach(region -> region.ifPresent(MapRegion::releaseTexture));
		}
	}

	private static void save() {
		ticksSinceSave = 0;
		for (Dimension dimension : dimensions.values()) {
			for (Optional<MapRegion> cached : dimension.regions.values()) {
				if (cached.isEmpty()) {
					continue;
				}
				try {
					cached.get().save();
				} catch (IOException e) {
					Cassonade.LOGGER.warn("Could not save the world map", e);
				}
			}
		}
	}

	private static final class Dimension {
		private final ResourceKey<Level> key;
		/** Access-ordered cache of loaded regions and regions known to have no file. */
		private final LinkedHashMap<Long, Optional<MapRegion>> regions = new LinkedHashMap<>(16, 0.75F, true);
		private final RegionOverviews overviews;

		private Dimension(ResourceKey<Level> key) {
			this.key = key;
			this.overviews = new RegionOverviews(folder(), this::textureId);
		}

		private Path folder() {
			Identifier id = key.identifier();
			return folder.resolve(id.getNamespace()).resolve(id.getPath());
		}

		private Identifier textureId(int regionX, int regionZ) {
			Identifier id = key.identifier();
			return Cassonade.id("worldmap/" + id.getNamespace() + "/" + id.getPath() + "/" + regionX + "_" + regionZ);
		}
	}
}
