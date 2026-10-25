package com.cassonade.client.map;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.BitSet;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;

/**
 * 32 × 32 explored chunks of the world map: one packed map color per block (see {@link TerrainColors}),
 * saved deflated in its own file, and turned into a texture while the world map is open.
 */
public class MapRegion {
	public static final int CHUNKS = 32;
	public static final int BLOCKS = CHUNKS * 16;

	private final Path file;
	private final Identifier textureId;
	private final byte[] colors = new byte[BLOCKS * BLOCKS];
	/** Chunks changed since the texture was last updated. */
	private final BitSet staleChunks = new BitSet(CHUNKS * CHUNKS);
	private boolean unsaved;
	private DynamicTexture texture;

	MapRegion(Path file, Identifier textureId) {
		this.file = file;
		this.textureId = textureId;
	}

	static MapRegion load(Path file, Identifier textureId) throws IOException {
		MapRegion region = new MapRegion(file, textureId);
		if (Files.exists(file)) {
			try (InputStream in = new InflaterInputStream(Files.newInputStream(file))) {
				in.readNBytes(region.colors, 0, region.colors.length);
			}
			region.staleChunks.set(0, CHUNKS * CHUNKS);
		}
		return region;
	}

	void save() throws IOException {
		if (!unsaved) {
			return;
		}
		Files.createDirectories(file.getParent());
		// Written next to the region then moved, so a crash never leaves a truncated file
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		try (OutputStream out = new DeflaterOutputStream(Files.newOutputStream(temp))) {
			out.write(colors);
		}
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		unsaved = false;
	}

	/** Copies the 16 × 16 colors sampled for a chunk, given by its position within the region. */
	void setChunk(int chunkX, int chunkZ, byte[] chunkColors) {
		for (int z = 0; z < 16; z++) {
			System.arraycopy(chunkColors, z * 16, colors, chunkX * 16 + (chunkZ * 16 + z) * BLOCKS, 16);
		}
		staleChunks.set(chunkX + chunkZ * CHUNKS);
		unsaved = true;
	}

	/** The region's texture, created on first use and refreshed where chunks were explored since. */
	public Identifier texture() {
		if (texture == null) {
			texture = new DynamicTexture(textureId::toString, BLOCKS, BLOCKS, true);
			Minecraft.getInstance().getTextureManager().register(textureId, texture);
			staleChunks.set(0, CHUNKS * CHUNKS);
		}
		if (!staleChunks.isEmpty()) {
			NativeImage pixels = texture.getPixels();
			for (int chunk = staleChunks.nextSetBit(0); chunk >= 0; chunk = staleChunks.nextSetBit(chunk + 1)) {
				int minX = chunk % CHUNKS * 16;
				int minZ = chunk / CHUNKS * 16;
				for (int z = minZ; z < minZ + 16; z++) {
					for (int x = minX; x < minX + 16; x++) {
						pixels.setPixel(x, z, MapColor.getColorFromPackedId(colors[x + z * BLOCKS]));
					}
				}
			}
			staleChunks.clear();
			texture.upload();
		}
		return textureId;
	}

	/** Frees the texture; the colors stay so it can be made again. */
	public void releaseTexture() {
		if (texture != null) {
			Minecraft.getInstance().getTextureManager().release(textureId);
			texture = null;
		}
	}
}
