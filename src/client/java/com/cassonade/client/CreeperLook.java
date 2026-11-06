package com.cassonade.client;

import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/**
 * The creeper as a bush: leaves, branches and twigs sticking out of it and swaying, a ribcage of wood on its chest
 * whose hollow glimmers faintly, and glows more and more as it swells. Only the picture: its shape, hitbox, sounds and explosion stay the game's. Its
 * texture is drawn by tools/creeper_texture.py, which follows the boxes below.
 */
public final class CreeperLook extends RenderLayer<CreeperRenderState, CreeperModel> {
	private static final Identifier FUSE = Identifier.withDefaultNamespace("textures/entity/creeper/creeper_fuse.png");
	private static final Identifier HEART = Identifier.withDefaultNamespace("textures/entity/creeper/creeper_heart.png");
	/** Which of the spots a creeper grows something on, drawn from its UUID (CreeperRendererMixin). */
	public static final RenderStateDataKey<Long> SEED = RenderStateDataKey.create(() -> "cassonade:creeper_seed");
	/** Where leaves, branches and twigs may grow, each creeper showing about half of them. */
	private static final String[] SPOTS = {
			"large_sprout", "small_sprout", "side_leaf", "head_branch", "back_leaf", "head_twig",
			"shoulder_leaf", "low_leaf", "back_sprout", "side_branch", "back_branch", "low_twig",
	};
	/**
	 * The only face of a leaf, flat: the game shows it from both sides. Its back face would lie in the same plane, its
	 * texture the next leaf's, and the two would flicker in turn.
	 */
	private static final Set<Direction> FRONT = Set.of(Direction.NORTH);
	/** At least this many spots grow, so no creeper is left bare. */
	private static final int FEWEST = 3;

	private CreeperLook(RenderLayerParent<CreeperRenderState, CreeperModel> renderer) {
		super(renderer);
	}

	public static void init() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof CreeperRenderer creeper) {
				helper.register(new CreeperLook(creeper));
			}
		});
	}

	/**
	 * Vanilla's boxes (CreeperModelMixin), with leaves, branches and twigs here and there, the leaves flat as the
	 * warden's tendrils, none matching another. The charged creeper's swirl inflates the boxes, so it gets none: they
	 * would swell into slabs. The parts keep vanilla's names, which the model looks up and animates.
	 */
	public static LayerDefinition bodyLayer(CubeDeformation inflation) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, inflation),
				PartPose.offset(0.0F, 6.0F, 0.0F));
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, inflation),
				PartPose.offset(0.0F, 6.0F, 0.0F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, inflation);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.0F, 18.0F, 4.0F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(2.0F, 18.0F, 4.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.0F, 18.0F, -4.0F));
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(2.0F, 18.0F, -4.0F));
		if (inflation != CubeDeformation.NONE) {
			return LayerDefinition.create(mesh, 64, 32);
		}

		// On the head: a large sprout leaning left, a small one leaning back, a leaf out of each side, a branch with a
		// leaf at its tip, a bare twig at the front corner
		head.addOrReplaceChild("large_sprout", CubeListBuilder.create().texOffs(32, 0).addBox(-2.5F, -6.0F, 0.0F, 5.0F, 6.0F, 0.0F, FRONT),
				PartPose.offsetAndRotation(-1.5F, -8.0F, -1.0F, -0.15F, 0.3F, -0.6F));
		head.addOrReplaceChild("small_sprout", CubeListBuilder.create().texOffs(37, 0).addBox(-2.0F, -5.0F, 0.0F, 4.0F, 5.0F, 0.0F, FRONT),
				PartPose.offsetAndRotation(1.5F, -8.0F, 2.0F, -0.5F, -0.4F, 0.25F));
		head.addOrReplaceChild("side_leaf", CubeListBuilder.create().texOffs(32, 6).addBox(0.0F, -1.5F, 0.0F, 6.0F, 3.0F, 0.0F, FRONT),
				PartPose.offsetAndRotation(4.0F, -6.0F, 1.0F, 0.0F, 0.4F, 0.3F));
		head.addOrReplaceChild("back_leaf", CubeListBuilder.create().texOffs(38, 6).addBox(-6.0F, -1.5F, 0.0F, 6.0F, 3.0F, 0.0F, FRONT),
				PartPose.offsetAndRotation(-4.0F, -3.0F, 2.5F, 0.0F, 0.5F, -0.4F));
		PartDefinition headBranch = head.addOrReplaceChild("head_branch", CubeListBuilder.create().texOffs(32, 10).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(2.5F, -8.0F, -1.5F, -0.2F, 0.0F, 0.55F));
		headBranch.addOrReplaceChild("leaf", CubeListBuilder.create().texOffs(44, 6).addBox(0.0F, -1.5F, 0.0F, 5.0F, 3.0F, 0.0F, FRONT),
				PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.0F, 0.3F, -0.5F));
		head.addOrReplaceChild("head_twig", CubeListBuilder.create().texOffs(36, 10).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offsetAndRotation(-3.5F, -8.0F, -3.5F, -0.3F, 0.0F, -0.4F));
		// On the body: a leaf drooping from the right shoulder, a smaller one lower on the left, a sprout behind the left
		// shoulder, a branch out of each of its left side and back with a leaf at its tip, a bare twig low on the right
		body.addOrReplaceChild("shoulder_leaf", CubeListBuilder.create().texOffs(38, 6).addBox(-6.0F, -1.5F, 0.0F, 6.0F, 3.0F, 0.0F, FRONT),
				PartPose.offsetAndRotation(-4.0F, 1.0F, 0.5F, 0.0F, -0.3F, -0.7F));
		body.addOrReplaceChild("low_leaf", CubeListBuilder.create().texOffs(44, 6).addBox(0.0F, -1.5F, 0.0F, 5.0F, 3.0F, 0.0F, FRONT),
				PartPose.offsetAndRotation(4.0F, 6.0F, -0.5F, 0.0F, 0.5F, 0.9F));
		body.addOrReplaceChild("back_sprout", CubeListBuilder.create().texOffs(41, 0).addBox(-2.5F, -6.0F, 0.0F, 5.0F, 6.0F, 0.0F, FRONT),
				PartPose.offsetAndRotation(2.0F, 0.0F, 1.5F, 0.5F, 0.2F, 0.3F));
		PartDefinition sideBranch = body.addOrReplaceChild("side_branch", CubeListBuilder.create().texOffs(40, 10).addBox(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
				PartPose.offsetAndRotation(4.0F, 4.0F, 0.0F, 0.0F, 0.3F, -0.6F));
		sideBranch.addOrReplaceChild("leaf", CubeListBuilder.create().texOffs(37, 0).addBox(-2.0F, -5.0F, 0.0F, 4.0F, 5.0F, 0.0F, FRONT),
				PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F));
		PartDefinition backBranch = body.addOrReplaceChild("back_branch", CubeListBuilder.create().texOffs(40, 12).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F),
				PartPose.offsetAndRotation(-1.5F, 3.0F, 2.0F, 0.6F, -0.3F, 0.0F));
		backBranch.addOrReplaceChild("leaf", CubeListBuilder.create().texOffs(32, 6).addBox(0.0F, -1.5F, 0.0F, 6.0F, 3.0F, 0.0F, FRONT),
				PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.0F, -1.2F, -0.3F));
		body.addOrReplaceChild("low_twig", CubeListBuilder.create().texOffs(50, 10).addBox(-3.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
				PartPose.offsetAndRotation(-4.0F, 8.0F, 0.5F, 0.0F, 0.4F, 0.5F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	/** The spots of a model, found once by their names; empty for the charged creeper's swirl, which has none. */
	public static @Nullable ModelPart[] spots(ModelPart root) {
		Function<String, @Nullable ModelPart> lookup = root.createPartLookup();
		ModelPart[] parts = new ModelPart[SPOTS.length];
		for (int i = 0; i < SPOTS.length; i++) {
			parts[i] = lookup.apply(SPOTS[i]);
		}
		return parts;
	}

	/**
	 * Shows this creeper's spots, about half of them, always the same for it (three bits of its seed for each), each
	 * leaning a little its own way, and sets them swaying: slowly at rest, bouncing as it walks, shivering as it swells.
	 */
	public static void grow(@Nullable ModelPart[] spots, CreeperRenderState state) {
		long seed = state.getDataOrDefault(SEED, 0L);
		long bits = mix(seed);
		int shown = 0;
		for (int i = 0; i < spots.length; i++) {
			boolean grows = ((bits >>> (3 * i)) & 7) < 4;
			shown += grows ? 1 : 0;
			if (spots[i] != null) {
				spots[i].visible = grows;
			}
		}
		// Too few: the first ones the seed skipped grow too
		for (int i = 0; i < spots.length && shown < FEWEST; i++) {
			if (spots[i] != null && !spots[i].visible) {
				spots[i].visible = true;
				shown++;
			}
		}

		float swelling = Mth.clamp(state.swelling, 0.0F, 1.0F);
		for (int i = 0; i < spots.length; i++) {
			ModelPart spot = spots[i];
			if (spot == null || !spot.visible) {
				continue;
			}
			// Its own lean, up to a quarter of a radian each way on two axes, and its own pace
			long own = mix(seed + 31L * (i + 1));
			float phase = (own & 0xFF) / 255.0F * Mth.TWO_PI;
			spot.xRot += lean(own >>> 8);
			spot.zRot += lean(own >>> 16);
			float sway = Mth.sin(state.ageInTicks * 0.07F + phase) * 0.06F
					+ Mth.cos(state.walkAnimationPos * 1.3F + phase) * 0.25F * state.walkAnimationSpeed
					+ Mth.sin(state.ageInTicks * 2.5F + phase) * 0.1F * swelling;
			spot.zRot += sway;
			spot.xRot += sway * 0.5F;
		}
	}

	/** Eight bits as an angle between -0.25 and 0.25. */
	private static float lean(long bits) {
		return ((bits & 0xFF) / 255.0F - 0.5F) * 0.5F;
	}

	/** SplitMix64's finalizer, so that UUIDs close to each other still give unrelated spots. */
	private static long mix(long seed) {
		long z = seed + 0x9E3779B97F4A7C15L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CreeperRenderState state, float yRot, float xRot) {
		if (state.isInvisible) {
			return;
		}
		// The glows are added to the skin, as spiders' eyes are, so they are dimmed by their color rather than their alpha.
		// The heart breathes, barely: a glimmer in the dark, not a lantern, gone in the light, where it would only redden
		// the hollow
		ClientLevel level = Minecraft.getInstance().level;
		int skyDarken = level != null ? level.getSkyDarken() : 0;
		int light = Math.max(LightCoordsUtil.block(lightCoords), Math.max(0, LightCoordsUtil.sky(lightCoords) - skyDarken));
		float shade = 1.0F - light / 15.0F;
		if (shade > 0.0F) {
			float breath = 0.5F + 0.5F * Mth.sin(state.ageInTicks * 0.08F + (state.getDataOrDefault(SEED, 0L) & 0xFF));
			glow(submitNodeCollector, poseStack, state, HEART, (0.12F + 0.1F * breath) * shade * shade);
		}
		float swelling = Mth.clamp(state.swelling, 0.0F, 1.0F);
		if (swelling > 0.0F) {
			// Embers waking up: faint at the first hiss, bright when it blows, flickering a little on the way
			float flicker = 0.85F + 0.15F * Mth.sin(state.ageInTicks * 2.3F);
			glow(submitNodeCollector, poseStack, state, FUSE, Mth.sqrt(swelling) * flicker);
		}
	}

	private void glow(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, CreeperRenderState state, Identifier texture, float brightness) {
		int level = Math.round(255 * Mth.clamp(brightness, 0.0F, 1.0F));
		submitNodeCollector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(texture),
				LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ARGB.color(255, level, level, level), null, state.outlineColor);
	}
}
