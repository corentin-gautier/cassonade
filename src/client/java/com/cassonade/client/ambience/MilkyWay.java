package com.cassonade.client.ambience;

import org.joml.Matrix3f;
import org.joml.Vector3f;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The galaxy across the night, turning with the stars (SkyRendererMixin), built as the game builds them: small stars over
 * large faint quads that merge into a glow. Shaped as the real one: tilted 63 degrees to the axis the sky turns on, its
 * bright bulge high in the sky at midnight, thinning away from it, split along the middle by a dark rift.
 */
public final class MilkyWay {
	private static final float DISTANCE = 100;
	/** In the stars' frame, where the sky turns on X and midnight's zenith is -Y: the bulge, 29 degrees off the celestial equator. */
	private static final Vector3f CORE = new Vector3f(-0.485F, -0.875F, 0).normalize();
	/** The band's axis: 63 degrees from the sky's, and square to the bulge. */
	private static final Vector3f NORMAL = new Vector3f(0.456F, -0.2527F, 0.853F).normalize();
	private static final Vector3f SIDE = new Vector3f(NORMAL).cross(CORE).normalize();
	private static final int STARS = 3200;
	private static final int HAZE = 3600;
	private static final int BULGE = 900;
	/** Fainter than the game's stars: seen on a second look, as in a real night sky. */
	public static final float STAR_BRIGHTNESS = 0.6F;
	public static final float HAZE_BRIGHTNESS = 0.13F;
	public static final float BULGE_BRIGHTNESS = 0.16F;
	/** The cold blue-violet of the band, and the warmer light of its heart. */
	public static final Vector3f HAZE_COLOR = new Vector3f(0.62F, 0.62F, 1);
	public static final Vector3f BULGE_COLOR = new Vector3f(1, 0.82F, 0.72F);

	private MilkyWay() {
	}

	public static GpuBuffer buildStars() {
		return build("Milky Way stars", STARS, 0.08F, 0.1F, 0.11F, false, 451L);
	}

	public static GpuBuffer buildHaze() {
		return build("Milky Way haze", HAZE, 0.9F, 1.2F, 0.13F, false, 877L);
	}

	public static GpuBuffer buildBulge() {
		return build("Milky Way bulge", BULGE, 1.2F, 1.6F, 0.15F, true, 233L);
	}

	public static int starCount() {
		return STARS;
	}

	public static int hazeCount() {
		return HAZE;
	}

	public static int bulgeCount() {
		return BULGE;
	}

	public static int indexCount(int stars) {
		return stars * 6;
	}

	private static GpuBuffer build(String name, int count, float minSize, float sizeRange, float spread, boolean bulgeOnly, long seed) {
		RandomSource random = RandomSource.createThreadLocalInstance(seed);
		Vector3f up = new Vector3f(0, 1, 0);
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize() * count * 4)) {
			BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION);
			int placed = 0;
			while (placed < count) {
				// Radians along the band from the bulge, and across it
				float along = (random.nextFloat() * 2 - 1) * Mth.PI;
				float nearBulge = (float) Math.exp(-along * along / 0.3F);
				float across = (float) random.nextGaussian() * spread * (0.55F + 1.1F * nearBulge);
				float keep = bulgeOnly ? nearBulge : (0.35F + 0.65F * nearBulge) * (0.7F + 0.3F * Mth.sin(along * 9 + across * 20));
				if (random.nextFloat() > keep || inRift(along, across) && random.nextFloat() < 0.85F) {
					continue;
				}
				float inPlane = Mth.cos(across);
				Vector3f center = new Vector3f(CORE).mul(Mth.cos(along) * inPlane)
						.add(new Vector3f(SIDE).mul(Mth.sin(along) * inPlane))
						.add(new Vector3f(NORMAL).mul(Mth.sin(across)))
						.normalize(DISTANCE);
				float size = minSize + random.nextFloat() * sizeRange;
				Matrix3f rotation = new Matrix3f().rotateTowards(new Vector3f(center).negate(), up).rotateZ(-random.nextFloat() * Mth.TWO_PI);
				builder.addVertex(new Vector3f(size, -size, 0).mul(rotation).add(center));
				builder.addVertex(new Vector3f(size, size, 0).mul(rotation).add(center));
				builder.addVertex(new Vector3f(-size, size, 0).mul(rotation).add(center));
				builder.addVertex(new Vector3f(-size, -size, 0).mul(rotation).add(center));
				placed++;
			}
			try (MeshData mesh = builder.buildOrThrow()) {
				return RenderSystem.getDevice().createBuffer(() -> name, 40, mesh.vertexBuffer());
			}
		}
	}

	/** The dark lane, wandering along the middle of the band on the bulge's side of the sky. */
	private static boolean inRift(float along, float across) {
		if (Math.abs(along) > 1.9F) {
			return false;
		}
		float middle = 0.03F * Mth.sin(along * 2.3F + 0.7F) + 0.015F * Mth.sin(along * 6.1F);
		return Math.abs(across - middle) < 0.035F + 0.018F * Mth.sin(along * 4.7F);
	}
}
