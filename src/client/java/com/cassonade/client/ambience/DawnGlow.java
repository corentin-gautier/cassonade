package com.cassonade.client.ambience;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import com.cassonade.Cassonade;

/**
 * The glow of sunrise and sunset, in place of the game's fan: 16 sides whose straight edges show on the night sky, an
 * alpha falling in a straight line to its rim, which draws a crease where the glow stops, and only a band low over the
 * horizon, which stopped short in the sky. This one has 64 sides, reaches six times as high, and fades on a curve that
 * dies out slowly, so the glow climbs far up the sky with nothing marking where it ends. Drawn with the game's color and
 * transforms (SkyRendererMixin).
 */
public final class DawnGlow {
	private static final int SIDES = 64;
	private static final int RINGS = 8;
	/** The game's fan: from the sky's height out to a rim 120 wide around the origin. The game's rim leaves the horizon by a third of its radius, this one by twice it. */
	private static final float HEIGHT = 100;
	private static final float RADIUS = 120;
	private static final float TILT = 2.0F;
	/** Triangles, the game's fan being one triangle fan: those around the center, then two per cell of each outer ring. */
	public static final int VERTICES = SIDES * 3 + (RINGS - 1) * SIDES * 6;
	/** The game's sunrise pipeline, in triangles rather than one fan. */
	public static final RenderPipeline PIPELINE = builder("pipeline/dawn_glow").build();
	/**
	 * The same, only where the horizon's disc was drawn just before, writing its depth nearer than the glow (SkyRendererMixin):
	 * the disc hides the sun, moon and stars under the horizon, and this brings the glow back over it. Covered there, the glow
	 * stopped on the disc's edge, and far land, fading into the sky drawn behind it, showed that edge as a line across it.
	 */
	public static final RenderPipeline BELOW_HORIZON = builder("pipeline/dawn_glow_below_horizon")
			.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
			.build();
	/** The game's sky pipeline, writing a depth for BELOW_HORIZON to test against. */
	public static final RenderPipeline HORIZON = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
			.withLocation(Cassonade.id("pipeline/horizon"))
			.withVertexShader("core/sky")
			.withFragmentShader("core/sky")
			.withVertexBinding(0, DefaultVertexFormat.POSITION)
			.withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
			.withColorTargetState(ColorTargetState.DEFAULT)
			.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
			.build();

	private DawnGlow() {
	}

	private static RenderPipeline.Builder builder(String location) {
		return RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
				.withLocation(Cassonade.id(location))
				.withBindGroupLayout(BindGroupLayouts.PROJECTION)
				.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
				.withVertexShader("core/position_color")
				.withFragmentShader("core/position_color")
				.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
				.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
				.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
				.withCull(false);
	}

	public static GpuBuffer build() {
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(VERTICES * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
			BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
			for (int ring = 1; ring <= RINGS; ring++) {
				for (int i = 0; i < SIDES; i++) {
					if (ring == 1) {
						vertex(builder, 0, i);
					} else {
						vertex(builder, ring - 1, i);
					}
					vertex(builder, ring, i);
					vertex(builder, ring, i + 1);
					if (ring > 1) {
						vertex(builder, ring - 1, i);
						vertex(builder, ring, i + 1);
						vertex(builder, ring - 1, i + 1);
					}
				}
			}
			try (MeshData mesh = builder.buildOrThrow()) {
				return RenderSystem.getDevice().createBuffer(() -> "Cassonade dawn glow", 32, mesh.vertexBuffer());
			}
		}
	}

	private static void vertex(BufferBuilder builder, int ring, int side) {
		float t = (float) ring / RINGS;
		float angle = side * Mth.TWO_PI / SIDES;
		float radius = RADIUS * t;
		// Flat at both ends, where a straight slope would show a crease
		float alpha = (1 - t) * (1 - t);
		builder.addVertex(Mth.sin(angle) * radius, HEIGHT * (1 - t) + Mth.cos(angle) * radius, -Mth.cos(angle) * radius * TILT)
				.setColor(ARGB.white(alpha));
	}
}
