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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Rarely, as a clear day ends, a great cloud of starlings comes in, folds and swells in the sky for about a minute, and
 * goes on. As with {@link Birds}, nothing but a picture on the client. Hundreds of birds are far too many for models, and
 * at this distance each is a few pixels: two flat quads facing the player, moved by waves rather than by simulating a flock.
 */
public final class Murmuration {
	/** The hour before sunset, tried once a day, for a player under the open sky. */
	private static final int EVENING_START = 11000;
	private static final int EVENING_END = 12000;
	/** One evening in this many. */
	private static final int ODDS = 8;
	private static final int STARLINGS = 1600;
	private static final int DURATION = 1600;
	/** From the start of the path to its middle, most of it flown in the first and last few seconds. */
	private static final float REACH = 320;
	private static final float MIN_ASIDE = 55;
	private static final float MAX_ASIDE = 95;
	private static final float MIN_ABOVE = 45;
	private static final float MAX_ABOVE = 75;
	private static final float RADIUS = 38;
	/** Half a wing, in blocks. */
	private static final float WING = 0.5F;
	private static final float BODY = 0.17F;
	private static final float FLAP = 0.28F;
	private static final float FLAP_SPEED = 0.9F;
	private static final int COLOR = 0x1C1A20;
	private static final RandomSource RANDOM = RandomSource.create();
	private static boolean spawning = true;
	private static long triedDay = Long.MIN_VALUE;
	private static @Nullable Cloud cloud;

	private Murmuration() {
	}

	static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null) {
			cloud = null;
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		if (cloud != null && (++cloud.age >= DURATION || cloud.level != level)) {
			cloud = null;
		}
		if (spawning && cloud == null && canGather(minecraft)) {
			long day = Math.floorDiv(level.getDefaultClockTime(), (long) Ambience.TICKS_PER_DAY);
			if (day != triedDay) {
				triedDay = day;
				if (RANDOM.nextInt(ODDS) == 0) {
					release(minecraft);
				}
			}
		}
	}

	/** For tests, through {@link Birds#setSpawning}: stops clouds coming on their own, and empties the sky, or lets them come again. */
	static void setSpawning(boolean spawning) {
		Murmuration.spawning = spawning;
		cloud = null;
	}

	/** Clear weather under the open sky, as for any flock, in the last hour before sunset. */
	public static boolean canGather(Minecraft minecraft) {
		if (!Birds.canFly(minecraft)) {
			return false;
		}
		long tick = Ambience.tickOfDay(minecraft.level);
		return tick >= EVENING_START && tick < EVENING_END;
	}

	public static void release(Minecraft minecraft) {
		cloud = new Cloud(minecraft.level, minecraft.player.getEyePosition());
	}

	public static boolean isGathered() {
		return cloud != null;
	}

	/** Where the middle of the cloud is now, in the world; null with none in the sky. */
	public static @Nullable Vec3 position() {
		return cloud == null ? null : cloud.position(cloud.age);
	}

	static void draw(LevelRenderContext context) {
		Cloud shown = cloud;
		if (shown == null || shown.level != Minecraft.getInstance().level) {
			return;
		}
		float time = shown.age + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		float alpha = shown.alpha(time);
		if (alpha <= 0) {
			return;
		}
		Vec3 center = shown.position(time).subtract(context.levelState().cameraRenderState.pos);
		PoseStack poseStack = context.poseStack();
		context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> shown.draw(pose, buffer, center, time, alpha));
	}

	private static final class Cloud {
		final Level level;
		final Vec3 middle;
		final float headingX;
		final float headingZ;
		/** Each bird's place in a ball of radius 1, and its own moment in the wingbeats. */
		final float[] seeds = new float[STARLINGS * 4];
		int age;

		Cloud(Level level, Vec3 eye) {
			this.level = level;
			float heading = RANDOM.nextFloat() * Mth.TWO_PI;
			headingX = Mth.cos(heading);
			headingZ = Mth.sin(heading);
			float aside = Mth.randomBetween(RANDOM, MIN_ASIDE, MAX_ASIDE) * (RANDOM.nextBoolean() ? 1 : -1);
			middle = eye.add(-headingZ * aside, Mth.randomBetween(RANDOM, MIN_ABOVE, MAX_ABOVE), headingX * aside);
			for (int i = 0; i < STARLINGS; i++) {
				float along = RANDOM.nextFloat() * 2 - 1;
				float around = RANDOM.nextFloat() * Mth.TWO_PI;
				// Thicker towards the middle than an even ball, whose edge would read as a sphere
				float distance = RANDOM.nextFloat();
				float flat = Mth.sqrt(1 - along * along);
				seeds[i * 4] = distance * flat * Mth.cos(around);
				seeds[i * 4 + 1] = distance * along;
				seeds[i * 4 + 2] = distance * flat * Mth.sin(around);
				seeds[i * 4 + 3] = RANDOM.nextFloat() * Mth.TWO_PI;
			}
		}

		/** Fast at both ends of its way, nearly still in the middle: a cube of the progress from -1 to 1. */
		Vec3 position(float time) {
			float progress = progress(time);
			float along = progress * progress * progress * REACH;
			return middle.add(headingX * along, Mth.sin(time * 0.011F) * 6, headingZ * along);
		}

		float alpha(float time) {
			float fade = Mth.clamp((1 - Math.abs(progress(time))) / 0.1F, 0, 1);
			return fade * (1 - level.getRainLevel(1));
		}

		private static float progress(float time) {
			return time / DURATION * 2 - 1;
		}

		void draw(PoseStack.Pose pose, VertexConsumer buffer, Vec3 center, float time, float alpha) {
			// Quads turned to the camera, at the origin: a flat wing seen edge-on would vanish
			Vec3 toCamera = center.lengthSqr() < 1 ? new Vec3(0, 0, 1) : center.reverse().normalize();
			Vec3 right = toCamera.cross(new Vec3(0, 1, 0)).normalize();
			if (right.lengthSqr() < 0.5) {
				right = new Vec3(1, 0, 0);
			}
			Vec3 up = right.cross(toCamera);
			float rightX = (float) right.x;
			float rightY = (float) right.y;
			float rightZ = (float) right.z;
			float upX = (float) up.x;
			float upY = (float) up.y;
			float upZ = (float) up.z;
			float centerX = (float) center.x;
			float centerY = (float) center.y;
			float centerZ = (float) center.z;
			// The shape changes slowly, each axis swelling and thinning on its own: a ball, a sheet, a long ribbon
			float sizeX = RADIUS * (1.25F + 0.65F * Mth.sin(time * 0.013F));
			float sizeY = RADIUS * (0.55F + 0.3F * Mth.sin(time * 0.017F + 2));
			float sizeZ = RADIUS * (0.8F + 0.45F * Mth.sin(time * 0.011F + 4));
			float turn = time * 0.007F;
			float cosTurn = Mth.cos(turn);
			float sinTurn = Mth.sin(turn);
			float tilt = Mth.sin(time * 0.009F) * 0.6F;
			float cosTilt = Mth.cos(tilt);
			float sinTilt = Mth.sin(tilt);
			int color = ARGB.color(Mth.floor(alpha * 255), COLOR);
			for (int i = 0; i < STARLINGS; i++) {
				float a = seeds[i * 4];
				float b = seeds[i * 4 + 1];
				float c = seeds[i * 4 + 2];
				float phase = seeds[i * 4 + 3];
				// A wave running through the birds, strong enough to fold them over each other: the dark bands of a real flock
				float x = a + 0.38F * Mth.sin(4.2F * a + 1.3F * c - time * 0.075F);
				float y = b + 0.5F * Mth.sin(2.1F * x + time * 0.031F);
				float z = c + 0.5F * Mth.sin(2.3F * y - time * 0.023F + 1);
				x *= sizeX;
				y *= sizeY;
				z *= sizeZ;
				float tilted = y * cosTilt - x * sinTilt;
				float flat = x * cosTilt + y * sinTilt;
				float worldX = centerX + flat * cosTurn - z * sinTurn;
				float worldY = centerY + tilted;
				float worldZ = centerZ + flat * sinTurn + z * cosTurn;
				// As the wave passes they all turn together, and each shows less of its wings: the cloud pales and darkens
				float wing = WING * (0.65F + 0.35F * Mth.sin(2.8F * a + 1.9F * b - time * 0.06F));
				float flap = Mth.sin(time * FLAP_SPEED + phase) * FLAP;
				wing(pose, buffer, worldX, worldY, worldZ, -wing, flap, rightX, rightY, rightZ, upX, upY, upZ, color);
				wing(pose, buffer, worldX, worldY, worldZ, wing, flap, rightX, rightY, rightZ, upX, upY, upZ, color);
			}
		}

		private static void wing(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float out, float flap, float rightX, float rightY, float rightZ, float upX, float upY, float upZ, int color) {
			float tipX = x + rightX * out + upX * flap;
			float tipY = y + rightY * out + upY * flap;
			float tipZ = z + rightZ * out + upZ * flap;
			buffer.addVertex(pose, x + upX * BODY, y + upY * BODY, z + upZ * BODY).setColor(color);
			buffer.addVertex(pose, tipX + upX * BODY * 0.4F, tipY + upY * BODY * 0.4F, tipZ + upZ * BODY * 0.4F).setColor(color);
			buffer.addVertex(pose, tipX - upX * BODY * 0.4F, tipY - upY * BODY * 0.4F, tipZ - upZ * BODY * 0.4F).setColor(color);
			buffer.addVertex(pose, x - upX * BODY, y - upY * BODY, z - upZ * BODY).setColor(color);
		}
	}
}
