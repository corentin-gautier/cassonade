package com.vanillasugar.client.map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import com.vanillasugar.VanillaSugar;

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

	private static Path folder;
	private static final Map<ResourceKey<Level>, Dimension> dimensions = new HashMap<>();
	private static final ArrayDeque<long[]> queue = new ArrayDeque<>();
	private static final byte[] chunkColors = new byte[16 * 16];
	private static int ticksSinceSave;

	private WorldMap() {
	}

	public static void open(Minecraft minecraft) {
		close();
		if (minecraft.getSingleplayerServer() != null) {
			folder = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve("vanillasugar/worldmap");
		} else {
			ServerData server = minecraft.getCurrentServer();
			String name = server == null ? "unknown" : server.ip.replaceAll("[^A-Za-z0-9._-]", "_");
			folder = minecraft.gameDirectory.toPath().resolve("vanillasugar/worldmap/servers").resolve(name);
		}
	}

	public static void close() {
		save();
		releaseTextures();
		dimensions.clear();
		queue.clear();
		folder = null;
	}

	public static void tick(Minecraft minecraft) {
		if (folder == null || minecraft.level == null || minecraft.player == null) {
			return;
		}

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
			// The client also holds chunks whose blocks have not arrived yet: they must not erase what was explored
			if (!isBlank(chunkColors)) {
				MapRegion region = region(level.dimension(), chunkX >> 5, chunkZ >> 5, true);
				if (region != null) {
					region.setChunk(chunkX & 31, chunkZ & 31, chunkColors);
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
		MapRegion region = dimension.regions.get(key);
		if (region != null || !create && dimension.missing.contains(key)) {
			return region;
		}

		Path file = dimension.folder().resolve("r." + regionX + "." + regionZ + ".bin");
		if (!create && !Files.exists(file)) {
			dimension.missing.add(key);
			return null;
		}
		try {
			region = MapRegion.load(file, dimension.textureId(regionX, regionZ));
		} catch (IOException e) {
			VanillaSugar.LOGGER.warn("Could not read world map region {}", file, e);
			region = new MapRegion(file, dimension.textureId(regionX, regionZ));
		}
		dimension.regions.put(key, region);
		dimension.missing.remove(key);
		return region;
	}

	/** Frees the textures made for the world map screen. */
	public static void releaseTextures() {
		for (Dimension dimension : dimensions.values()) {
			dimension.regions.values().forEach(MapRegion::releaseTexture);
		}
	}

	private static void save() {
		ticksSinceSave = 0;
		for (Dimension dimension : dimensions.values()) {
			for (MapRegion region : dimension.regions.values()) {
				try {
					region.save();
				} catch (IOException e) {
					VanillaSugar.LOGGER.warn("Could not save the world map", e);
				}
			}
		}
	}

	private static final class Dimension {
		private final ResourceKey<Level> key;
		private final Long2ObjectMap<MapRegion> regions = new Long2ObjectOpenHashMap<>();
		/** Regions known to have no file, so the world map screen does not look for them every frame. */
		private final LongSet missing = new LongOpenHashSet();

		private Dimension(ResourceKey<Level> key) {
			this.key = key;
		}

		private Path folder() {
			Identifier id = key.identifier();
			return folder.resolve(id.getNamespace()).resolve(id.getPath());
		}

		private Identifier textureId(int regionX, int regionZ) {
			Identifier id = key.identifier();
			return VanillaSugar.id("worldmap/" + id.getNamespace() + "/" + id.getPath() + "/" + regionX + "_" + regionZ);
		}
	}
}
