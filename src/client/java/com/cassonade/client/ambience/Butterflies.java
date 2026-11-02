package com.cassonade.client.ambience;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;

/**
 * By clear day, a few butterflies around the flowers near the player. A model and nothing more: they cannot be touched,
 * and last less than a minute, flitting around the flower they came to.
 */
public final class Butterflies {
	private static final Identifier TEXTURE = Cassonade.id("textures/entity/butterfly.png");
	private static final int MAX = 6;
	private static final int LOOK_EVERY = 20;
	private static final int COLUMNS_TRIED = 8;
	private static final int RANGE = 14;
	private static final int MIN_LIFE = 500;
	private static final int MAX_LIFE = 1000;
	private static final int FADE = 30;
	private static final float SCALE = 0.32F;
	private static final float TEMPERATE_MIN = 0.3F;
	private static final int[] COLORS = {0xF29A2E, 0xF4EFE0, 0xF2D64B, 0x6FA8E8};
	private static final RandomSource RANDOM = RandomSource.create();
	private static final List<Butterfly> BUTTERFLIES = new ArrayList<>();
	private static int look;

	private Butterflies() {
	}

	public static int count() {
		return BUTTERFLIES.size();
	}

	/** The first one in the air; for tests. */
	public static Vec3 first() {
		return BUTTERFLIES.getFirst().position(0);
	}

	/** Some in the air right away, already faded in and hardly leaving these places; for tests. */
	public static void release(ClientLevel level, List<Vec3> anchors) {
		for (Vec3 anchor : anchors) {
			Butterfly butterfly = new Butterfly(level, anchor);
			butterfly.age = FADE;
			butterfly.reach = 0.1F;
			BUTTERFLIES.add(butterfly);
		}
	}

	static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null) {
			BUTTERFLIES.clear();
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		Vec3 player = minecraft.player.position();
		BUTTERFLIES.removeIf(b -> ++b.age >= b.life || b.anchor.distanceToSqr(player) > 48 * 48 || b.level != level);
		if (++look < LOOK_EVERY) {
			return;
		}
		look = 0;
		if (BUTTERFLIES.size() >= MAX || !Ambience.overworld(level) || !level.isBrightOutside() || level.isRaining()) {
			return;
		}
		BlockPos around = minecraft.player.blockPosition();
		for (int i = 0; i < COLUMNS_TRIED; i++) {
			int x = around.getX() + Mth.randomBetweenInclusive(RANDOM, -RANGE, RANGE);
			int z = around.getZ() + Mth.randomBetweenInclusive(RANDOM, -RANGE, RANGE);
			// The surface's first block is just above a flower, or the flower itself under a shorter measure
			BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			BlockPos flower = level.getBlockState(top).is(BlockTags.FLOWERS) ? top : top.below();
			if (level.getBlockState(flower).is(BlockTags.FLOWERS) && level.getBiome(flower).value().getBaseTemperature() >= TEMPERATE_MIN) {
				BUTTERFLIES.add(new Butterfly(level, Vec3.atCenterOf(flower).add(0, 0.8, 0)));
				return;
			}
		}
	}

	static void draw(LevelRenderContext context) {
		if (BUTTERFLIES.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 camera = context.levelState().cameraRenderState.pos;
		PoseStack pose = context.poseStack();
		RenderType type = RenderTypes.entityCutout(TEXTURE);
		for (Butterfly butterfly : BUTTERFLIES) {
			butterfly.draw(pose, context, type, camera, partialTick);
		}
	}

	private static final class Butterfly {
		final ClientLevel level;
		final Vec3 anchor;
		final int life = Mth.randomBetweenInclusive(RANDOM, MIN_LIFE, MAX_LIFE);
		final int color = COLORS[RANDOM.nextInt(COLORS.length)];
		final float phase = RANDOM.nextFloat() * Mth.TWO_PI;
		final ModelPart root = model().bakeRoot();
		final ModelPart left = root.getChild("left");
		final ModelPart right = root.getChild("right");
		int age;
		float reach = 1;

		Butterfly(ClientLevel level, Vec3 anchor) {
			this.level = level;
			this.anchor = anchor;
		}

		Vec3 position(float partialTick) {
			float t = age + partialTick + phase * 40;
			return anchor.add((Mth.sin(t * 0.031F) * 1.6F + Mth.sin(t * 0.083F) * 0.5F) * reach,
					(Mth.sin(t * 0.057F) * 0.5F + Mth.sin(t * 0.17F) * 0.12F) * reach,
					(Mth.cos(t * 0.027F) * 1.6F + Mth.sin(t * 0.071F) * 0.5F) * reach);
		}

		void draw(PoseStack pose, LevelRenderContext context, RenderType type, Vec3 camera, float partialTick) {
			Vec3 now = position(partialTick);
			Vec3 ahead = position(partialTick + 1);
			float time = age + partialTick;
			float appear = Mth.clamp(Math.min(time, life - time) / FADE, 0, 1);
			if (appear <= 0) {
				return;
			}
			float flap = (Mth.sin(time * 1.2F + phase) * 0.5F + 0.5F) * 1.2F + 0.1F;
			left.zRot = -flap;
			right.zRot = flap;
			pose.pushPose();
			pose.translate(now.x - camera.x, now.y - camera.y, now.z - camera.z);
			pose.rotate(Axis.YP.rotation((float) Mth.atan2(ahead.x - now.x, ahead.z - now.z)));
			float scale = SCALE * appear;
			pose.scale(-scale, -scale, scale);
			int light = LightCoordsUtil.pack(0, level.getBrightness(LightLayer.SKY, BlockPos.containing(now)));
			context.submitNodeCollector().submitModelPart(root, pose, type, light, OverlayTexture.NO_OVERLAY, null, ARGB.opaque(color));
			pose.popPose();
		}
	}

	private static LayerDefinition model() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 8).addBox(-0.5F, -0.5F, -2, 1, 1, 4), PartPose.ZERO);
		// Flat wings, as the game makes a bee's
		mesh.getRoot().addOrReplaceChild("left", CubeListBuilder.create().texOffs(-6, 0).addBox(0, 0, -3, 6, 0, 6), PartPose.offset(0.5F, 0, 0));
		mesh.getRoot().addOrReplaceChild("right", CubeListBuilder.create().texOffs(-6, 0).mirror().addBox(-6, 0, -3, 6, 0, 6), PartPose.offset(-0.5F, 0, 0));
		return LayerDefinition.create(mesh, 32, 16);
	}
}
