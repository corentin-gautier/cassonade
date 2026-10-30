package com.cassonade.client.map;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;

import org.jspecify.annotations.Nullable;

/**
 * 32 × 32 explored chunks of the world map: one packed map color per block and a surface biome per
 * 4 × 4 block cell, saved deflated in its own file, and turned into a texture while the world map is open.
 */
public class MapRegion {
	public static final int CHUNKS = 32;
	public static final int BLOCKS = CHUNKS * 16;
	/** The side of a region's overview, a pixel per 4 × 4 blocks. */
	public static final int OVERVIEW_SIZE = BLOCKS / 4;
	private static final int MAGIC = 0x43415332;
	private static final int BIOME_STEP = 4;
	private static final int BIOME_BLOCKS = BLOCKS / BIOME_STEP;

	private final Path file;
	private final Identifier textureId;
	private final byte[] colors = new byte[BLOCKS * BLOCKS];
	private final char[] biomeIndexes = new char[BIOME_BLOCKS * BIOME_BLOCKS];
	private final List<Identifier> biomePalette = new ArrayList<>();
	private final Map<Identifier, Integer> biomeLookup = new HashMap<>();
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
			try (DataInputStream in = new DataInputStream(new InflaterInputStream(Files.newInputStream(file)))) {
				int header = in.readInt();
				if (header == MAGIC) {
					in.readFully(region.colors);
					int paletteSize = in.readUnsignedShort();
					for (int i = 0; i < paletteSize; i++) {
						Identifier biome = Identifier.parse(in.readUTF());
						region.biomePalette.add(biome);
						region.biomeLookup.put(biome, i + 1);
					}
					for (int i = 0; i < region.biomeIndexes.length; i++) {
						region.biomeIndexes[i] = in.readChar();
					}
				} else {
					region.colors[0] = (byte) (header >>> 24);
					region.colors[1] = (byte) (header >>> 16);
					region.colors[2] = (byte) (header >>> 8);
					region.colors[3] = (byte) header;
					in.readFully(region.colors, 4, region.colors.length - 4);
				}
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
		try (DataOutputStream out = new DataOutputStream(new DeflaterOutputStream(Files.newOutputStream(temp)))) {
			out.writeInt(MAGIC);
			out.write(colors);
			out.writeShort(biomePalette.size());
			for (Identifier biome : biomePalette) {
				out.writeUTF(biome.toString());
			}
			for (char biomeIndex : biomeIndexes) {
				out.writeChar(biomeIndex);
			}
		}
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		unsaved = false;
	}

	/** Copies the colors and surface biomes sampled for a chunk, given by its position within the region. */
	void setChunk(int chunkX, int chunkZ, byte[] chunkColors, Identifier[] chunkBiomes) {
		for (int z = 0; z < 16; z++) {
			System.arraycopy(chunkColors, z * 16, colors, chunkX * 16 + (chunkZ * 16 + z) * BLOCKS, 16);
		}
		for (int z = 0; z < 16 / BIOME_STEP; z++) {
			for (int x = 0; x < 16 / BIOME_STEP; x++) {
				Identifier biome = chunkBiomes[x + z * (16 / BIOME_STEP)];
				if (biome == null) {
					continue;
				}
				int index = biomeLookup.computeIfAbsent(biome, key -> {
					biomePalette.add(key);
					return biomePalette.size();
				});
				int biomeX = chunkX * (16 / BIOME_STEP) + x;
				int biomeZ = chunkZ * (16 / BIOME_STEP) + z;
				biomeIndexes[biomeX + biomeZ * BIOME_BLOCKS] = (char) index;
			}
		}
		staleChunks.set(chunkX + chunkZ * CHUNKS);
		unsaved = true;
	}

	@Nullable Identifier biomeAt(int blockX, int blockZ) {
		int biomeIndex = biomeIndexes[blockX / BIOME_STEP + blockZ / BIOME_STEP * BIOME_BLOCKS];
		return biomeIndex == 0 ? null : biomePalette.get(biomeIndex - 1);
	}

	/** The most common map color of each 4 × 4 blocks: an average would invent colors and blur the explored edge. */
	byte[] overview() {
		byte[] overview = new byte[OVERVIEW_SIZE * OVERVIEW_SIZE];
		int[] counts = new int[256];
		for (int cellZ = 0; cellZ < OVERVIEW_SIZE; cellZ++) {
			for (int cellX = 0; cellX < OVERVIEW_SIZE; cellX++) {
				int best = 0;
				for (int z = cellZ * 4; z < cellZ * 4 + 4; z++) {
					for (int x = cellX * 4; x < cellX * 4 + 4; x++) {
						int color = colors[x + z * BLOCKS] & 0xFF;
						if (color != 0 && ++counts[color] > counts[best]) {
							best = color;
						}
					}
				}
				for (int z = cellZ * 4; z < cellZ * 4 + 4; z++) {
					for (int x = cellX * 4; x < cellX * 4 + 4; x++) {
						counts[colors[x + z * BLOCKS] & 0xFF] = 0;
					}
				}
				overview[cellX + cellZ * OVERVIEW_SIZE] = (byte) best;
			}
		}
		return overview;
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
