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
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ParticleStatus;
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
 * surface along the water's edge, with drops of water thrown up where it breaks. Waves times them to the loudest moments of its sound.
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
	private static final int DAY = ARGB.color(255, 232, 232, 240);
	/** A moonlit blue: white merely dimmed with the light turns a dirty grey on the dark water. */
	private static final int NIGHT = ARGB.color(255, 80, 108, 170);
	/** How dark the sky gets at the dead of night. */
	private static final float DARKEST_SKY = 11;
	/** One edge in so many breaks with a splash, or the whole shore would drizzle at once. */
	private static final int SPLASH_RARITY = 3;
	private static final int DROPS_PER_SPLASH = 4;
	/** The game's splash is blue through and through and gets lost on the water: white chips, thrown higher, make it read as spray. */
	private static final int SPRAY_PER_SPLASH = 8;
	private static final int SPRAY_LIFE = 16;
	/** As the foam nears the top of its run. */
	private static final int SPLASH_FROM = LIFE / 4;
	private static final int SPLASH_TO = LIFE / 2;
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
	private static final List<Splash> SPLASHES = new ArrayList<>();

	private record Patch(float x, float y, float z, float towardsX, float towardsZ, float size, int delay) {
	}

	private record Splash(float x, float y, float z, float towardsX, float towardsZ, int delay) {
	}

	private static int age = LIFE;

	private Foam() {
	}

	static void tick(ClientLevel level) {
		if (PATCHES.isEmpty()) {
			return;
		}
		if (++age >= LIFE) {
			PATCHES.clear();
			SPLASHES.clear();
			return;
		}
		for (Splash splash : SPLASHES) {
			if (splash.delay == age) {
				splash(level, splash);
			}
		}
	}

	/** Lays a line of foam along the sea's edge around the player. */
	public static void surge(Minecraft minecraft, ClientLevel level) {
		PATCHES.clear();
		SPLASHES.clear();
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
		if (PATCHES.size() < MAX_PATCHES && RANDOM.nextInt(SPLASH_RARITY) == 0) {
			// Right against the land, where the wave meets it
			SPLASHES.add(new Splash(water.getX() + 0.5F + land.getStepX() * 0.4F, water.getY() + SURFACE, water.getZ() + 0.5F + land.getStepZ() * 0.4F,
					land.getStepX(), land.getStepZ(), Mth.randomBetweenInclusive(RANDOM, SPLASH_FROM, SPLASH_TO)));
		}
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

	private static void splash(ClientLevel level, Splash splash) {
		for (int i = 0; i < DROPS_PER_SPLASH; i++) {
			float along = RANDOM.nextFloat() - 0.5F;
			// Any upward speed but zero: the particle then picks its own leap, higher than the fixed hop it makes otherwise
			level.addParticle(ParticleTypes.SPLASH, splash.x + splash.towardsZ * along, splash.y, splash.z + splash.towardsX * along, 0, 0.1, 0);
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
			return;
		}
		for (int i = 0; i < SPRAY_PER_SPLASH; i++) {
			float along = RANDOM.nextFloat() - 0.5F;
			Particle spray = minecraft.particleEngine.createParticle(ParticleTypes.ITEM_SNOWBALL, splash.x + splash.towardsZ * along, splash.y, splash.z + splash.towardsX * along, 0, 0, 0);
			if (spray != null) {
				float forward = Mth.randomBetween(RANDOM, 0, 0.06F);
				float aside = Mth.randomBetween(RANDOM, -0.03F, 0.03F);
				spray.setParticleSpeed(splash.towardsX * forward + splash.towardsZ * aside, Mth.randomBetween(RANDOM, 0.15F, 0.35F), splash.towardsZ * forward + splash.towardsX * aside);
				spray.setLifetime(SPRAY_LIFE);
			}
		}
	}

	public static int patches() {
		return PATCHES.size();
	}

	public static int splashes() {
		return SPLASHES.size();
	}

	static void draw(LevelRenderContext context) {
		if (PATCHES.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		float time = age + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		int color = ARGB.srgbLerp(Mth.clamp(minecraft.level.getSkyDarken() / DARKEST_SKY, 0, 1), DAY, NIGHT);
		if (renderType == null) {
			renderType = RenderTypeAccessor.cassonade$create("cassonade_foam", RenderSetup.builder(PIPELINE).createRenderSetup());
		}
		Vec3 camera = context.levelState().cameraRenderState.pos;
		PoseStack poseStack = context.poseStack();
		context.submitNodeCollector().submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
			for (Patch patch : PATCHES) {
				draw(pose, buffer, patch, time, color, camera);
			}
		});
	}

	private static void draw(PoseStack.Pose pose, VertexConsumer buffer, Patch patch, float time, int color, Vec3 camera) {
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
		buffer.addVertex(pose, x, y, z).setColor(color);
		buffer.addVertex(pose, x, y, z + size).setColor(color);
		buffer.addVertex(pose, x + size, y, z + size).setColor(color);
		buffer.addVertex(pose, x + size, y, z).setColor(color);
	}
}
