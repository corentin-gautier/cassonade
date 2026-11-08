package com.cassonade.client.stable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

import com.cassonade.stable.HitchingPostBlock;
import com.cassonade.stable.Stables;

/**
 * A lead's knot on a hitching post: a ring lying around the end of the rail, in the lead knot's texture, instead of the
 * game's upright knot, made to wrap a fence's post. It stops short of the post, so no face of it lies on the post's.
 */
public final class PostKnot {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/lead_knot/lead_knot.png");
	/** In the block's pixels, the post facing north: around the rail's west end (y 11 to 14, z 6.5 to 9.5), the post from x 5. */
	private static final ModelPart KNOT = knot();

	private PostKnot() {
	}

	/** Its faces laid out one pixel in from the texture's corner, where they all fall on the lead's opaque pixels. */
	private static ModelPart knot() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("knot", CubeListBuilder.create().texOffs(1, 1).addBox(1.5F, 10, 5.5F, 3, 5, 5), PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 32).bakeRoot();
	}

	/** Draws the knot of the post it is tied to, if it is on one: false leaves it to the game. */
	public static boolean submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		Minecraft minecraft = Minecraft.getInstance();
		BlockPos pos = BlockPos.containing(state.x, state.y, state.z);
		BlockState block = minecraft.level == null ? null : minecraft.level.getBlockState(pos);
		if (block == null || !block.is(Stables.BLOCK)) {
			return false;
		}
		poseStack.pushPose();
		// From the knot to the block's corner, then turned about its middle as the block's model is; a model's cubes are in pixels already
		poseStack.translate(pos.getX() - state.x, pos.getY() - state.y, pos.getZ() - state.z);
		poseStack.translate(0.5, 0, 0.5);
		poseStack.rotateDegrees(Axis.YP, -(block.getValue(HitchingPostBlock.FACING).toYRot() + 180));
		poseStack.translate(-0.5, 0, -0.5);
		collector.submitModelPart(KNOT, poseStack, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null, -1,
				state.outlineColor);
		poseStack.popPose();
		return true;
	}
}
