package com.cassonade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

import net.minecraft.core.Direction;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.AABB;

/**
 * Spiders drawn upright against the wall they climb, belly to it and head up, instead of lying flat in the air beside it.
 * Only the picture, on the client: the game already syncs whether a spider climbs, and its hitbox stays the same.
 */
public final class ClimbingSpiders {
	/** Spiders and cave spiders are drawn this much smaller than their hitbox, and their rider sits as much lower. */
	public static final float SCALE = 0.6F;
	/** How far up it is turned, 0 to 1. */
	public static final RenderStateDataKey<Float> CLIMB = RenderStateDataKey.create(() -> "cassonade:spider_climb");
	/** Per tick: a quarter of a turn in five ticks, so that a step climbed in passing only tips it. */
	public static final float TURN = 0.2F;
	/** How far the wall is looked for beyond the hitbox. */
	private static final double REACH = 0.1;
	/** Its body runs from 11 pixels ahead of its feet to 15 behind: raised to sit halfway up its hitbox once upright, in the model's own size. */
	private static final float LIFT = 0.55F;

	private ClimbingSpiders() {
	}

	/** Kept per spider by SpiderMixin. */
	public interface Climber {
		float cassonade$climb(float partialTicks);

		float cassonade$wallYaw();
	}

	/** The body's yaw facing the wall beside it, the one nearest the way it faces when it touches two, or NaN with none. */
	public static float wallYaw(Spider spider) {
		AABB box = spider.getBoundingBox();
		float best = Float.NaN;
		float bestTurn = Float.MAX_VALUE;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (spider.level().noCollision(spider, box.move(side.getStepX() * REACH, 0, side.getStepZ() * REACH))) {
				continue;
			}
			float yaw = side.toYRot();
			float turn = Math.abs(Mth.wrapDegrees(yaw - spider.yBodyRot));
			if (turn < bestTurn) {
				best = yaw;
				bestTurn = turn;
			}
		}
		return best;
	}

	/** Faces the wall and looks ahead, up it, the more the higher it is turned. */
	public static void extract(Spider spider, LivingEntityRenderState state, float partialTicks) {
		Climber climber = (Climber) spider;
		float climb = climber.cassonade$climb(partialTicks);
		state.setData(CLIMB, climb > 0 ? climb : null);
		if (climb > 0) {
			state.bodyRot = Mth.rotLerp(climb, state.bodyRot, climber.cassonade$wallYaw());
			state.yRot *= 1 - climb;
			state.xRot *= 1 - climb;
		}
	}

	/** After the body's yaw: brought to the wall, raised, then pitched up about its feet. */
	public static void turn(LivingEntityRenderState state, PoseStack poseStack) {
		Float climb = state.getData(CLIMB);
		if (climb == null || state.deathTime > 0) {
			return;
		}
		// Ahead is -z here: the model's front, once it is turned to face the body's yaw. The pose is already scaled
		// with the model, which is drawn smaller than the hitbox: the wall is half a hitbox away in world units
		poseStack.translate(0, LIFT * climb, -state.boundingBoxWidth / 2 / state.scale * climb);
		poseStack.rotateDegrees(Axis.XP, 90 * climb);
	}
}
