package com.cassonade.client.ambience;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Now and then on a clear night, a trail of square sparks crosses part of the sky in under a second. It stays at the same
 * distance around the camera, as the moon does, and the terrain hides it.
 */
public final class ShootingStars {
	private static final float RADIUS = 300;
	private static final int NIGHT_FROM = 13000;
	private static final int NIGHT_UNTIL = 23000;
	private static final int MIN_WAIT = 20 * 30;
	private static final int MAX_WAIT = 20 * 90;
	/** How soon to look again when the sky is not clear. */
	private static final int RETRY = 100;
	private static final int LIFE = 16;
	private static final float SPEED = 6;
	private static final int SPARKS = 14;
	private static final float SPARK_SPACING = 1.6F;
	private static final float SPARK_SIZE = 0.6F;
	private static final int COLOR = 0xFFF6E0;
	private static final RandomSource RANDOM = RandomSource.create();
	private static int wait = MIN_WAIT;
	private static @Nullable Star star;

	private ShootingStars() {
	}

	static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null) {
			star = null;
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		if (star != null && ++star.age >= LIFE) {
			star = null;
		}
		if (--wait <= 0) {
			if (canFall(level)) {
				release();
				wait = Mth.randomBetweenInclusive(RANDOM, MIN_WAIT, MAX_WAIT);
			} else {
				wait = RETRY;
			}
		}
	}

	public static boolean canFall(ClientLevel level) {
		long tick = Ambience.tickOfDay(level);
		return Ambience.overworld(level) && tick >= NIGHT_FROM && tick < NIGHT_UNTIL && !level.isRaining();
	}

	/** Somewhere high in the sky, falling a little as it crosses. */
	private static void release() {
		release(RANDOM.nextFloat() * Mth.TWO_PI, Mth.randomBetween(RANDOM, 30, 70));
	}

	/** Starting from this bearing, 0 to the north, and this many degrees above the horizon; for tests, to look at it. */
	public static void release(float bearing, float degrees) {
		float elevation = degrees * Mth.DEG_TO_RAD;
		float flat = Mth.cos(elevation);
		Vec3 start = new Vec3(Mth.sin(bearing) * flat, Mth.sin(elevation), -Mth.cos(bearing) * flat);
		Vec3 across = start.cross(new Vec3(0, 1, 0)).normalize().scale(RANDOM.nextBoolean() ? 1 : -1);
		Vec3 heading = across.add(0, -Mth.randomBetween(RANDOM, 0.2F, 0.7F), 0).normalize();
		star = new Star(start.scale(RADIUS), heading);
	}

	public static boolean isFalling() {
		return star != null;
	}

	static void draw(LevelRenderContext context) {
		Star shown = star;
		if (shown == null) {
			return;
		}
		float time = shown.age + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		PoseStack poseStack = context.poseStack();
		context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> shown.draw(pose, buffer, time));
	}

	private static final class Star {
		final Vec3 start;
		final Vec3 heading;
		int age;

		Star(Vec3 start, Vec3 heading) {
			this.start = start;
			this.heading = heading;
		}

		void draw(PoseStack.Pose pose, VertexConsumer buffer, float time) {
			Vec3 right = start.cross(new Vec3(0, 1, 0)).normalize().scale(SPARK_SIZE);
			Vec3 up = right.cross(start).normalize().scale(SPARK_SIZE);
			// Lights up at once, and dies out over its last moments
			float fade = Mth.clamp((LIFE - time) / (LIFE * 0.4F), 0, 1);
			for (int i = 0; i < SPARKS; i++) {
				float along = time * SPEED - i * SPARK_SPACING;
				if (along < 0) {
					break;
				}
				Vec3 spark = start.add(heading.scale(along));
				int color = ARGB.color(Mth.floor(255 * fade * (1 - i / (float) SPARKS)), COLOR);
				vertex(pose, buffer, spark.subtract(right).add(up), color);
				vertex(pose, buffer, spark.add(right).add(up), color);
				vertex(pose, buffer, spark.add(right).subtract(up), color);
				vertex(pose, buffer, spark.subtract(right).subtract(up), color);
			}
		}

		private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 at, int color) {
			buffer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(color);
		}
	}
}
