package com.cassonade.client.waystone;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;
import com.cassonade.waystone.WaystoneBlock;
import com.cassonade.waystone.WaystoneCrystalBlockEntity;

/**
 * What floats on top of the waystone moves as an active conduit does (ConduitRenderer), lit by itself: a cube turning on
 * a slanted axis and bobbing, its letters spelling PORTAL.
 */
public class WaystoneCrystalRenderer implements BlockEntityRenderer<WaystoneCrystalBlockEntity, WaystoneCrystalRenderer.State> {
	private static final SpriteId CORE = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Cassonade.id("block/waystone_crystal"));
	private static final Vector3f SLANT = new Vector3f(0.5F, 1, 0.5F).normalize();
	private static final float RADIANS_PER_TICK = -0.0375F;
	private static final float FLOAT_SPEED = 0.1F;
	private static final float FLOAT_HEIGHT = 1.5F / 16;
	/** The cube 6 pixels wide. */
	private static final float SCALE = 0.75F;

	public static class State extends BlockEntityRenderState {
		float turn;
		float floating;
	}

	private final SpriteGetter sprites;
	private final ModelPart core = cube().bakeRoot();

	public WaystoneCrystalRenderer(BlockEntityRendererProvider.Context context) {
		sprites = context.sprites();
	}

	private static LayerDefinition cube() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8), PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 16);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(WaystoneCrystalBlockEntity entity, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, camera, breakProgress);
		// In doubles, then wrapped: the game's time soon gets too large for a float's fractions
		double time = entity.getLevel().getGameTime() + (double) partialTicks;
		state.turn = (float) (time * RADIANS_PER_TICK % Mth.TWO_PI);
		float wave = (float) Math.sin(time * FLOAT_SPEED) / 2 + 0.5F;
		state.floating = (wave * wave + wave) * FLOAT_HEIGHT;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = LightCoordsUtil.FULL_BRIGHT;
		pose.pushPose();
		pose.translate(0.5F, WaystoneBlock.CRYSTAL_HEIGHT, 0.5F);
		pose.scale(SCALE, SCALE, SCALE);

		pose.translate(0, state.floating, 0);
		pose.rotate(new Quaternionf().rotationAxis(state.turn, SLANT));
		collector.submitModelPart(core, pose, CORE.renderType(RenderTypes::entityTranslucentEmissive), light, OverlayTexture.NO_OVERLAY, sprites.get(CORE));
		pose.popPose();
	}
}
