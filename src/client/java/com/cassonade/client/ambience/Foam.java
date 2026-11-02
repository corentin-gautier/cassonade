package com.cassonade.client.ambience;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;
import com.cassonade.client.mixin.RenderTypeAccessor;

/**
 * As a wave is heard, a line of foam runs up the shore and back, the water itself staying still: small flat pixels lying on the
 * surface along the water's edge. Waves times them to the loudest moments of its sound.
 */
public final class Foam {
	private static final int REACH = 22;
	private static final int PATCHES_PER_EDGE = 8;
	private static final int MAX_PATCHES = 1200;
	private static final int LIFE = 70;
	/** How far from the edge it starts, and how far up towards the land it runs. */
	private static final float SPREAD = 1.1F;
	private static final float RUN = 0.8F;
	/** The surface of a full water block, just above it. */
	private static final float SURFACE = 0.9F;
	/** A pixel in a block's sixteen, the size of the game's own. */
	private static final float PIXEL = 1 / 16F;
	private static final int WHITE = 232;
	/** Solid and writing depth: drawn before the water, which would otherwise tint it the water's own blue. */
	private static final RenderPipeline PIPELINE = RenderPipeline.builder()
			.withLocation(Cassonade.id("pipeline/foam"))
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
			.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withVertexShader("core/position_color")
			.withFragmentShader("core/position_color")
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
			.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withCull(false)
			.build();
	private static @Nullable RenderType renderType;
	private static final RandomSource RANDOM = RandomSource.create();
	private static final List<Patch> PATCHES = new ArrayList<>();

	private record Patch(float x, float y, float z, float towardsX, float towardsZ, float size, int delay) {
	}

	private static int age = LIFE;

	private Foam() {
	}

	static void tick(ClientLevel level) {
		if (!PATCHES.isEmpty() && ++age >= LIFE) {
			PATCHES.clear();
		}
	}

	/** Lays a line of foam along the sea's edge around the player. */
	public static void surge(Minecraft minecraft, ClientLevel level) {
		PATCHES.clear();
		age = 0;
		BlockPos around = minecraft.player.blockPosition();
		for (int x = around.getX() - REACH; x <= around.getX() + REACH; x++) {
			for (int z = around.getZ() - REACH; z <= around.getZ() + REACH; z++) {
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
				BlockPos water = new BlockPos(x, top, z);
				if (level.getFluidState(water).is(FluidTags.WATER) && AirSounds.coast(level, water)) {
					for (Direction side : Direction.Plane.HORIZONTAL) {
						int landX = x + side.getStepX();
						int landZ = z + side.getStepZ();
						int rise = level.getHeight(Heightmap.Types.MOTION_BLOCKING, landX, landZ) - 1 - top;
						if (rise >= 0 && rise <= Waves.MAX_BANK && !level.getFluidState(new BlockPos(landX, top + rise, landZ)).is(FluidTags.WATER)) {
							edge(water, side);
						}
					}
				}
			}
		}
	}

	private static void edge(BlockPos water, Direction land) {
		for (int i = 0; i < PATCHES_PER_EDGE && PATCHES.size() < MAX_PATCHES; i++) {
			float along = RANDOM.nextFloat() - 0.5F;
			float off = 0.5F - RANDOM.nextFloat() * SPREAD;
			float x = water.getX() + 0.5F + land.getStepX() * off + land.getStepZ() * along;
			float z = water.getZ() + 0.5F + land.getStepZ() * off + land.getStepX() * along;
			// Snapped to the block's pixels, as anything drawn in the game lines up
			PATCHES.add(new Patch(Math.round(x * 16) * PIXEL, water.getY() + SURFACE, Math.round(z * 16) * PIXEL, land.getStepX(), land.getStepZ(),
					Mth.randomBetweenInclusive(RANDOM, 3, 8) * PIXEL, RANDOM.nextInt(LIFE / 5)));
		}
	}

	public static int patches() {
		return PATCHES.size();
	}

	static void draw(LevelRenderContext context) {
		if (PATCHES.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		float time = age + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		// Lit by hand, this render type knowing nothing of the light: it must not glow at night
		int shade = Mth.floor(WHITE * Mth.clamp(1 - minecraft.level.getSkyDarken() / 13F, 0.15F, 1));
		if (renderType == null) {
			renderType = RenderTypeAccessor.cassonade$create("cassonade_foam", RenderSetup.builder(PIPELINE).createRenderSetup());
		}
		Vec3 camera = context.levelState().cameraRenderState.pos;
		PoseStack poseStack = context.poseStack();
		context.submitNodeCollector().submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
			for (Patch patch : PATCHES) {
				draw(pose, buffer, patch, time, shade, camera);
			}
		});
	}

	private static void draw(PoseStack.Pose pose, VertexConsumer buffer, Patch patch, float time, int shade, Vec3 camera) {
		float progress = Mth.clamp((time - patch.delay) / (LIFE - patch.delay), 0, 1);
		float wave = Mth.sin(progress * Mth.PI);
		// No fading, as nothing in the game's pixels fades: it grows and shrinks by whole pixels, and moves by them
		float size = Mth.floor(patch.size * wave / PIXEL) * PIXEL;
		if (size <= 0) {
			return;
		}
		float run = Mth.floor(wave * RUN / PIXEL) * PIXEL;
		float x = (float) (patch.x + patch.towardsX * run - camera.x);
		float y = (float) (patch.y - camera.y);
		float z = (float) (patch.z + patch.towardsZ * run - camera.z);
		int color = ARGB.color(255, shade, shade, Math.min(255, shade + 8));
		buffer.addVertex(pose, x, y, z).setColor(color);
		buffer.addVertex(pose, x, y, z + size).setColor(color);
		buffer.addVertex(pose, x + size, y, z + size).setColor(color);
		buffer.addVertex(pose, x + size, y, z).setColor(color);
	}
}
