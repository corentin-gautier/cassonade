package com.cassonade.client.ambience;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;

/**
 * Often by day, several flocks cross the sky above a player outdoors, each a V or a loose group of geese, a loose group of crows, or gulls over the coast, passing by and going on.
 * Nothing but a picture, with no entity behind it, drawn on the client alone so it works on any server; each player has
 * their own. They start far enough to be specks, so they neither pop in nor need a turn to leave the view.
 */
public final class Birds {
	/** Ticks between two flocks setting out: several cross the sky at once, on their own ways. */
	private static final int MIN_WAIT = 60;
	private static final int MAX_WAIT = 240;
	private static final int MAX_FLOCKS = 4;
	/** How often to look again once it is time, for a player indoors or a rainy sky. */
	private static final int RETRY = 200;
	private static final float SPEED = 0.45F;
	private static final float PATH_LENGTH = 400;
	/** The share of the path at each end over which the flock fades, too far to be seen fading. */
	private static final float FADE = 0.08F;
	private static final float MIN_ABOVE = 40;
	private static final float MAX_ABOVE = 200;
	private static final float MIN_ASIDE = 30;
	private static final float MAX_ASIDE = 90;
	private static final int MIN_BIRDS = 5;
	private static final int MAX_BIRDS = 13;
	private static final int MIN_GULLS = 3;
	private static final int MAX_GULLS = 7;
	private static final int MIN_CROWS = 4;
	private static final int MAX_CROWS = 9;
	/** One flock in this many, away from the coast. */
	private static final int CROW_ODDS = 3;
	private static final float CROW_SIZE = 0.75F;
	private static final float V_BACK = 2.2F;
	private static final float V_SIDE = 1.8F;
	private static final float LOOSE_LENGTH = 9;
	private static final float LOOSE_WIDTH = 6;
	/** A real skein of geese (sounds/CREDITS.md), without a subtitle: no vanilla sound is a wild bird's. */
	public static final Identifier CALL = Cassonade.id("birds");
	private static final float CALL_VOLUME = 0.18F;
	private static final float MIN_CALL_PITCH = 0.9F;
	private static final float MAX_CALL_PITCH = 1.1F;
	private static final int MIN_CALL_WAIT = 200;
	private static final int MAX_CALL_WAIT = 340;
	/** Beyond this distance, too far to be heard: a flock flying high passes in silence. */
	private static final double CALL_RANGE = 110;
	private static final Identifier TEXTURE = Cassonade.id("textures/entity/bird.png");
	private static final Identifier GULL_TEXTURE = Cassonade.id("textures/entity/gull.png");
	private static final Identifier CROW_TEXTURE = Cassonade.id("textures/entity/crow.png");
	/** Within the fog's reach, at low render distances too: seen through fog, they would be the color of the sky. */
	private static final float HEIGHT_MARGIN = 0.85F;
	private static final float FOG_MARGIN = 0.6F;
	private static final LayerDefinition MODEL = model();
	private static final RandomSource RANDOM = RandomSource.create();
	private static final List<Flock> FLOCKS = new ArrayList<>();
	private static boolean spawning = true;
	private static @Nullable RenderType birdType;
	private static @Nullable RenderType gullType;
	private static @Nullable RenderType crowType;
	private static int wait = randomWait();

	private Birds() {
	}

	static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null) {
			FLOCKS.clear();
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		FLOCKS.removeIf(flock -> ++flock.age >= PATH_LENGTH / SPEED || flock.level != level);
		// The nearest alone calls at a time: more than one would soon be a din. Crows have no call of their own, and fly by in silence
		Vec3 eye = minecraft.player == null ? Vec3.ZERO : minecraft.player.getEyePosition();
		FLOCKS.stream().filter(flock -> flock.kind != Kind.CROWS).min(Comparator.comparingDouble(flock -> flock.position(0).distanceToSqr(eye))).ifPresent(flock -> {
			if (--flock.nextCall <= 0) {
				flock.nextCall = Mth.randomBetweenInclusive(RANDOM, MIN_CALL_WAIT, MAX_CALL_WAIT);
				call(minecraft, flock);
			}
		});
		if (spawning && FLOCKS.size() < MAX_FLOCKS && --wait <= 0) {
			if (canFly(minecraft)) {
				release(minecraft);
				wait = randomWait();
			} else {
				wait = RETRY;
			}
		}
	}

	/** For tests: stops flocks coming on their own, and empties the sky, or lets them come again. */
	public static void setSpawning(boolean spawning) {
		Birds.spawning = spawning;
		FLOCKS.clear();
		Murmuration.setSpawning(spawning);
	}

	/** Clear daylight over the Overworld, for a player under the open sky who can look up and see them. */
	public static boolean canFly(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		return level != null && minecraft.player != null && level.dimension() == Level.OVERWORLD
				&& level.isBrightOutside() && !level.isRaining() && level.canSeeSky(BlockPos.containing(minecraft.player.getEyePosition()));
	}

	public static void release(Minecraft minecraft) {
		Kind kind = AirSounds.coast(minecraft.level, minecraft.player.blockPosition()) ? Kind.GULLS : RANDOM.nextInt(CROW_ODDS) == 0 ? Kind.CROWS : Kind.GEESE;
		add(minecraft, kind, Mth.randomBetween(RANDOM, MIN_ABOVE, MAX_ABOVE));
	}

	/** For tests: geese or, over the coast, gulls, at this height above the player. */
	public static void release(Minecraft minecraft, float above) {
		add(minecraft, AirSounds.coast(minecraft.level, minecraft.player.blockPosition()) ? Kind.GULLS : Kind.GEESE, above);
	}

	/** For tests: crows, at this height above the player. */
	public static void releaseCrows(Minecraft minecraft, float above) {
		add(minecraft, Kind.CROWS, above);
	}

	/** As far above as the fog lets them be seen. */
	private static void add(Minecraft minecraft, Kind kind, float above) {
		float highest = minecraft.options.getEffectiveRenderDistance() * 16 * HEIGHT_MARGIN;
		FLOCKS.add(new Flock(minecraft.level, minecraft.player.getEyePosition(), kind, Math.min(above, highest)));
	}

	public static int flockCount() {
		return FLOCKS.size();
	}

	public static boolean isFlying() {
		return !FLOCKS.isEmpty();
	}

	/** Where the last flock released is now, in the world; null with none in the sky. */
	public static @Nullable Vec3 position() {
		return FLOCKS.isEmpty() ? null : FLOCKS.getLast().position(0);
	}

	/**
	 * From where the flock is, so it can be placed by ear, but without the game's attenuation, which would silence it at
	 * this height: the volume follows the distance here instead. Not heard from indoors.
	 */
	private static void call(Minecraft minecraft, Flock flock) {
		Vec3 eye = minecraft.player.getEyePosition();
		Vec3 from = flock.position(0);
		float closeness = 1 - (float) (from.distanceTo(eye) / CALL_RANGE);
		if (closeness <= 0 || !minecraft.level.canSeeSky(BlockPos.containing(eye))) {
			return;
		}
		float pitch = Mth.randomBetween(RANDOM, MIN_CALL_PITCH, MAX_CALL_PITCH);
		// A late flock of gulls, crossing at nightfall, keeps quiet
		boolean gulls = flock.kind == Kind.GULLS;
		if (gulls && !Ambience.daytime(minecraft.level)) {
			return;
		}
		minecraft.getSoundManager().play(new SimpleSoundInstance(gulls ? AirSounds.GULLS : CALL, SoundSource.AMBIENT, (gulls ? AirSounds.GULL_VOLUME : CALL_VOLUME) * closeness * closeness, pitch,
				SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, from.x, from.y, from.z, false));
	}

	private static int randomWait() {
		return Mth.randomBetweenInclusive(RANDOM, MIN_WAIT, MAX_WAIT);
	}

	static void draw(LevelRenderContext context) {
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		for (Flock flying : FLOCKS) {
			if (flying.level == Minecraft.getInstance().level) {
				drawFlock(context, flying, partialTick);
			}
		}
	}

	private static void drawFlock(LevelRenderContext context, Flock flying, float partialTick) {
		Vec3 relative = flying.position(partialTick).subtract(context.levelState().cameraRenderState.pos);
		float time = flying.age + partialTick;
		int color = ARGB.color(flying.alpha(time), 0xFFFFFF);
		// Lit by the sky alone: darker as evening falls, and no torch below can light them
		int light = LightCoordsUtil.pack(0, 15);
		if (birdType == null) {
			birdType = unshaded(TEXTURE);
			gullType = unshaded(GULL_TEXTURE);
			crowType = unshaded(CROW_TEXTURE);
		}
		RenderType type = switch (flying.kind) {
			case GEESE -> birdType;
			case GULLS -> gullType;
			case CROWS -> crowType;
		};
		PoseStack pose = context.poseStack();
		float yaw = (float) Mth.atan2(flying.headingX, flying.headingZ);
		for (Bird bird : flying.birds) {
			bird.draw(pose, context, type, flying, relative, yaw, time, light, color);
		}
	}

	/**
	 * The breeze's wind, without a mob's shading from above: seen from below, as birds always are, that would turn a gull's
	 * white belly grey. The day's light still dims them at dusk. Made once, on the render thread: the game makes a new one each call.
	 */
	private static RenderType unshaded(Identifier texture) {
		return RenderTypes.breezeWind(texture, 0, 0);
	}

	private enum Kind {
		GEESE, GULLS, CROWS
	}

	private static final class Flock {
		final Level level;
		final Bird[] birds;
		/** Where the path comes closest to the player who saw it start. */
		final Vec3 middle;
		final float headingX;
		final float headingZ;
		/** Only geese fly in a V. */
		final Kind kind;
		int age;
		int nextCall;

		Flock(Level level, Vec3 eye, Kind kind, float above) {
			this.level = level;
			this.kind = kind;
			float heading = RANDOM.nextFloat() * Mth.TWO_PI;
			headingX = Mth.cos(heading);
			headingZ = Mth.sin(heading);
			float reach = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 * FOG_MARGIN;
			float aside = Math.min(reach * 0.7F, Mth.randomBetween(RANDOM, MIN_ASIDE, MAX_ASIDE)) * (RANDOM.nextBoolean() ? 1 : -1);
			middle = eye.add(-headingZ * aside, above, headingX * aside);
			boolean inV = kind == Kind.GEESE && RANDOM.nextBoolean();
			birds = new Bird[switch (kind) {
				case GEESE -> Mth.randomBetweenInclusive(RANDOM, MIN_BIRDS, MAX_BIRDS);
				case GULLS -> Mth.randomBetweenInclusive(RANDOM, MIN_GULLS, MAX_GULLS);
				case CROWS -> Mth.randomBetweenInclusive(RANDOM, MIN_CROWS, MAX_CROWS);
			}];
			float scale = kind == Kind.CROWS ? CROW_SIZE : 1;
			for (int i = 0; i < birds.length; i++) {
				birds[i] = inV ? Bird.inV(i) : Bird.loose(scale);
			}
		}

		Vec3 position(float partialTick) {
			float along = (age + partialTick) * SPEED - PATH_LENGTH / 2;
			return middle.add(headingX * along, 0, headingZ * along);
		}

		int alpha(float time) {
			float progress = time * SPEED / PATH_LENGTH;
			float fade = Mth.clamp(Math.min(progress, 1 - progress) / FADE, 0, 1);
			return Mth.floor(fade * (1 - level.getRainLevel(1)) * 255);
		}
	}

	/** Its place in the flock, along the way and across it; and where it is in its wingbeats, so they don't all beat together. */
	private static final class Bird {
		private static final float FLAP_SPEED = 0.55F;
		private static final float GLIDE_SPEED = 0.03F;
		/** How far the wings swing up and down, and the tip after them, in radians. */
		private static final float FLAP = 0.7F;
		private static final float TIP = 0.5F;
		/** Wings held slightly up, gliding. */
		private static final float GLIDE = 0.12F;
		private static final float DRIFT_SPEED = 0.05F;
		private static final float DRIFT = 0.35F;

		final float back;
		final float side;
		final float height;
		final float phase = RANDOM.nextFloat() * Mth.TWO_PI;
		final float size;
		// Its own model: the wings' angles are read when the frame is drawn, after every bird was submitted
		final ModelPart root = MODEL.bakeRoot();
		final ModelPart leftWing = root.getChild("left_wing");
		final ModelPart leftTip = leftWing.getChild("tip");
		final ModelPart rightWing = root.getChild("right_wing");
		final ModelPart rightTip = rightWing.getChild("tip");

		Bird(float back, float side, float height, float scale) {
			this.back = back;
			this.side = side;
			this.height = height;
			size = scale * Mth.randomBetween(RANDOM, 0.85F, 1.15F);
		}

		/** The lead bird first, then one on each side in turn, each rank further back. */
		static Bird inV(int index) {
			int rank = (index + 1) / 2;
			return new Bird(rank * V_BACK, rank * V_SIDE * (index % 2 == 0 ? 1 : -1), jitter(0.3F), 1);
		}

		static Bird loose(float scale) {
			return new Bird(jitter(LOOSE_LENGTH), jitter(LOOSE_WIDTH), jitter(1.5F), scale);
		}

		private static float jitter(float range) {
			return (RANDOM.nextFloat() * 2 - 1) * range;
		}

		void draw(PoseStack pose, LevelRenderContext context, RenderType type, Flock flock, Vec3 flockPos, float yaw, float time, int light, int color) {
			float rightX = -flock.headingZ;
			float rightZ = flock.headingX;
			float drift = Mth.sin(time * DRIFT_SPEED + phase) * DRIFT;
			// Beats, then glides a while, each on its own rhythm
			float beating = Mth.clamp(Mth.sin(time * GLIDE_SPEED + phase) * 2 + 1, 0, 1);
			float wave = Mth.sin(time * FLAP_SPEED + phase);
			float angle = Mth.lerp(beating, GLIDE, wave * FLAP);
			float tip = beating * Mth.sin(time * FLAP_SPEED + phase - 0.9F) * TIP;
			leftWing.zRot = -angle;
			leftTip.zRot = -tip;
			rightWing.zRot = angle;
			rightTip.zRot = tip;
			pose.pushPose();
			pose.translate(flockPos.x - flock.headingX * back + rightX * (side + drift),
					flockPos.y + height + Mth.cos(time * DRIFT_SPEED + phase) * DRIFT * 0.5F,
					flockPos.z - flock.headingZ * back + rightZ * (side + drift));
			pose.rotate(Axis.YP.rotation(yaw));
			pose.scale(-size, -size, size);
			context.submitNodeCollector().submitModelPart(root, pose, type, light, OverlayTexture.NO_OVERLAY, null, color);
			pose.popPose();
		}
	}

	/** One pixel a sixteenth of a block as for any mob, its head towards +Z; a wing of two parts, bending at the middle. */
	private static LayerDefinition model() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2, -1.5F, -4, 4, 3, 8), PartPose.ZERO);
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12).addBox(-1.5F, -2, 0, 3, 3, 3), PartPose.offset(0, -0.5F, 4));
		head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(12, 12).addBox(-0.5F, -1, 3, 1, 1, 2), PartPose.ZERO);
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 19).addBox(-2, -1, -8, 4, 1, 4), PartPose.ZERO);
		wing(root, "left_wing", 2, 0, 6);
		wing(root, "right_wing", -2, -6, -6);
		return LayerDefinition.create(mesh, 64, 32);
	}

	private static void wing(PartDefinition root, String name, float at, float from, float tipAt) {
		PartDefinition wing = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(24, 0).addBox(from, -0.5F, -3, 6, 1, 5), PartPose.offset(at, -1, 0));
		wing.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(24, 7).addBox(from, -0.5F, -3, 6, 1, 4), PartPose.offset(tipAt, 0, 0));
	}
}
