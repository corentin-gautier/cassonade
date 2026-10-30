package com.cassonade.client.map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;

import org.jspecify.annotations.Nullable;

import com.cassonade.Cassonade;

/**
 * The regions of a dimension in low resolution, for the world map zoomed far out, where a whole explored
 * world can show at once. They are read from disk away from the render thread and kept for the session, a
 * few textures made per frame, so the far zoom neither holds every region in memory nor stalls on them.
 */
final class RegionOverviews {
	private static final int MAX_TEXTURES_PER_FRAME = 32;
	private static final byte[] NOTHING_EXPLORED = new byte[0];
	private static final ExecutorService READER = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "Cassonade world map reader");
		thread.setDaemon(true);
		return thread;
	});

	private final Path folder;
	private final BiFunction<Integer, Integer, Identifier> regionTextureId;
	private final Map<Long, Overview> overviews = new HashMap<>();
	private int texturesLeft;

	RegionOverviews(Path folder, BiFunction<Integer, Integer, Identifier> regionTextureId) {
		this.folder = folder;
		this.regionTextureId = regionTextureId;
	}

	void startFrame() {
		texturesLeft = MAX_TEXTURES_PER_FRAME;
	}

	/** The texture to draw, if any; {@code live} is the region if it is loaded, more recent than its file. */
	@Nullable Identifier texture(int regionX, int regionZ, @Nullable MapRegion live) {
		long key = (long) regionX << 32 | regionZ & 0xFFFFFFFFL;
		Overview overview = overviews.computeIfAbsent(key, k -> {
			Overview created = new Overview(regionTextureId.apply(regionX, regionZ).withSuffix("_overview"));
			created.read(folder.resolve("r." + regionX + "." + regionZ + ".bin"));
			return created;
		});
		if (overview.stale && texturesLeft > 0) {
			if (live != null) {
				overview.colors = live.overview();
			} else {
				// Evicted since it changed, so saved: its file is up to date
				overview.read(folder.resolve("r." + regionX + "." + regionZ + ".bin"));
			}
			overview.stale = false;
			overview.textureStale = true;
		}
		byte[] colors = overview.colors;
		if (colors == null || colors == NOTHING_EXPLORED) {
			return null;
		}
		if ((overview.texture == null || overview.textureStale) && texturesLeft > 0) {
			texturesLeft--;
			overview.upload(colors);
		}
		return overview.texture == null ? null : overview.id;
	}

	void markStale(int regionX, int regionZ) {
		Overview overview = overviews.get((long) regionX << 32 | regionZ & 0xFFFFFFFFL);
		if (overview != null) {
			overview.stale = true;
		}
	}

	int pending() {
		return (int) overviews.values().stream().filter(overview -> overview.colors == null).count();
	}

	int drawn() {
		return (int) overviews.values().stream().filter(overview -> overview.texture != null).count();
	}

	void releaseTextures() {
		overviews.values().forEach(Overview::releaseTexture);
	}

	/** Drops the reads still waiting, the world having closed. */
	void abandon() {
		overviews.values().forEach(overview -> overview.abandoned = true);
		overviews.clear();
	}

	private static final class Overview {
		private final Identifier id;
		/** Written by the reader, null until it is done. */
		private volatile byte @Nullable [] colors;
		private volatile boolean abandoned;
		/** Explored since the colors were taken. */
		private boolean stale;
		private volatile boolean textureStale;
		private @Nullable DynamicTexture texture;

		private Overview(Identifier id) {
			this.id = id;
		}

		private void read(Path file) {
			READER.execute(() -> {
				if (abandoned) {
					return;
				}
				byte[] read = NOTHING_EXPLORED;
				try {
					if (Files.exists(file)) {
						read = MapRegion.load(file, id).overview();
					}
				} catch (IOException e) {
					Cassonade.LOGGER.warn("Could not read world map region {}", file, e);
				}
				colors = read;
				textureStale = true;
			});
		}

		private void upload(byte[] colors) {
			if (texture == null) {
				texture = new DynamicTexture(id::toString, MapRegion.OVERVIEW_SIZE, MapRegion.OVERVIEW_SIZE, true);
				Minecraft.getInstance().getTextureManager().register(id, texture);
			}
			NativeImage pixels = texture.getPixels();
			for (int z = 0; z < MapRegion.OVERVIEW_SIZE; z++) {
				for (int x = 0; x < MapRegion.OVERVIEW_SIZE; x++) {
					pixels.setPixel(x, z, MapColor.getColorFromPackedId(colors[x + z * MapRegion.OVERVIEW_SIZE]));
				}
			}
			texture.upload();
			textureStale = false;
		}

		private void releaseTexture() {
			if (texture != null) {
				Minecraft.getInstance().getTextureManager().release(id);
				texture = null;
			}
		}
	}
}
