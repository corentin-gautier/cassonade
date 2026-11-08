package com.cassonade.client.ambience;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Small birds perched somewhere near the player by day, a kind each (Robins on tree tops, Sparrows on what was built): one
 * hops about, looks around, sings or chirps from where it stands, and flies off as anyone comes near, closer for one
 * sneaking up on it. It comes in from beyond its perch, as seen from the player, slowing to land. A model and nothing more,
 * as the butterflies: no entity, nothing to touch, and each player sees their own.
 */
final class PerchedBirds {
	private static final int LOOK_EVERY = 40;
	private static final int COLUMNS_TRIED = 12;
	/** Ways in tried for a perch, the ones running into a block left aside. */
	private static final int WAYS_TRIED = 4;
	/** How near anyone may come before it flies off: farther for one running, closer for one sneaking. */
	private static final float FLEE = 6;
	private static final float FLEE_RUNNING = 9;
	private static final float FLEE_SNEAKING = 3;
	private static final float GONE = 64;
	private static final int MIN_STAY = 1200;
	private static final int MAX_STAY = 4800;
	/** Each tick at dusk or in the rain, the chance it leaves: they go one by one rather than all at once. */
	private static final int LEAVE_ODDS = 100;
	private static final float TAKEOFF_VOLUME = 0.5F;
	private static final float TAKEOFF_RANGE = 20;
	private static final int FLIGHT = 90;
	/** Comes in from far off, the way it would leave, slowing to land. */
	private static final int LANDING = 60;
	private static final float LANDING_VOLUME = 0.25F;
	/** Shrinks away at the end of its flight, far off by then, and grows the same way out of the distance as it comes in. */
	private static final int FADE = 15;
	private static final float TOP_SPEED = 0.45F;
	/** The ways tried to fly off, from straight away from whoever came near to back past them. */
	private static final float[] TURNS = {0, 0.6F, -0.6F, 1.2F, -1.2F, 1.8F, -1.8F, 2.4F, -2.4F, Mth.PI};
	/** How far it turns aside, in flight, from a block in its way. */
	private static final float[] AVOID_TURNS = {0.7F, 1.4F, 2.1F, 2.8F};
	private static final LayerDefinition MODEL = model();
	private static final RandomSource RANDOM = RandomSource.create();

	/** Where one stands, how far it may hop about there, and the block it stands on. */
	record Perch(Vec3 at, float roam, BlockPos block) {
	}

	/** Where a kind perches: the perch a column offers, if any, and whether a perch still holds one. */
	interface Perches {
		@Nullable Perch find(ClientLevel level, int x, int z);

		boolean holds(ClientLevel level, Perch perch);
	}

	/**
	 * What sets a kind apart: its look and size, how many, one look in how many bringing one, how close together, where and when they come, and their
	 * song, how loud, how often and how long.
	 */
	record Kind(Identifier texture, float scale, int max, int settleOdds, float nearest, float farthest, float apart, Perches perches, Predicate<Minecraft> comes,
			Identifier song, float songVolume, float songRange, int minSongWait, int maxSongWait, int singing, float takeOffPitch) {
	}

	private final Kind kind;
	private final List<Bird> birds = new ArrayList<>();
	private boolean spawning = true;
	private int look;

	PerchedBirds(Kind kind) {
		this.kind = kind;
	}

	int count() {
		return birds.size();
	}

	long flying() {
		return birds.stream().filter(bird -> bird.flight >= 0).count();
	}

	long landing() {
		return birds.stream().filter(bird -> bird.landing >= 0).count();
	}

	/** One coming in to land on this perch, from beyond it; false with every way in blocked. */
	boolean land(Minecraft minecraft, Perch perch) {
		Bird bird = arriving(minecraft.level, perch, minecraft.player.getEyePosition());
		if (bird != null) {
			birds.add(bird);
		}
		return bird != null;
	}

	/** Whether one is inside a block right now: none should be, perched or flying. */
	boolean anyInBlock() {
		return birds.stream().anyMatch(bird -> !bird.level.getBlockState(BlockPos.containing(bird.position(0).add(0, 0.2, 0))).isAir());
	}

	@Nullable Vec3 first() {
		return birds.isEmpty() ? null : birds.getFirst().position(0);
	}

	void setSpawning(boolean spawning) {
		this.spawning = spawning;
		birds.clear();
	}

	/** One on this perch, facing the player. */
	void perch(Minecraft minecraft, Perch perch) {
		Bird bird = new Bird(minecraft.level, perch);
		Vec3 eye = minecraft.player.getEyePosition();
		bird.yaw = (float) Mth.atan2(eye.x - perch.at.x, eye.z - perch.at.z);
		birds.add(bird);
	}

	/** One landing on a perch before the player's eyes, to watch it in runClient; false with none in sight. */
	boolean perchInView(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		Vec3 eye = minecraft.player.getEyePosition();
		Vec3 look = minecraft.player.getLookAngle();
		float bearing = (float) Mth.atan2(look.z, look.x);
		for (int distance = 5; distance <= 32; distance++) {
			for (float turn = 0; turn <= 0.5F; turn += 0.05F) {
				for (int side = -1; side <= 1; side += 2) {
					float angle = bearing + turn * side;
					Perch perch = kind.perches.find(level, Mth.floor(eye.x + Mth.cos(angle) * distance), Mth.floor(eye.z + Mth.sin(angle) * distance));
					if (perch != null) {
						if (!land(minecraft, perch)) {
							perch(minecraft, perch);
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null) {
			birds.clear();
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		Vec3 player = minecraft.player.position();
		boolean stay = Ambience.daytime(level) && !level.isRaining();
		birds.removeIf(bird -> bird.level != level || bird.perch.at.distanceToSqr(player) > GONE * GONE || bird.flight >= FLIGHT);
		for (Bird bird : birds) {
			bird.tick(minecraft, stay);
		}
		if (++look < LOOK_EVERY) {
			return;
		}
		look = 0;
		if (spawning && birds.size() < kind.max && RANDOM.nextInt(kind.settleOdds) == 0 && Ambience.overworld(level) && level.getBrightness(LightLayer.SKY, minecraft.player.blockPosition()) > 0
				&& kind.comes.test(minecraft)) {
			settle(minecraft, level);
		}
	}

	/** One flying in to a perch around the player, in sight or not, apart from the others. */
	private void settle(Minecraft minecraft, ClientLevel level) {
		Vec3 eye = minecraft.player.getEyePosition();
		for (int i = 0; i < COLUMNS_TRIED; i++) {
			float angle = RANDOM.nextFloat() * Mth.TWO_PI;
			float distance = Mth.randomBetween(RANDOM, kind.nearest, kind.farthest);
			Perch perch = kind.perches.find(level, Mth.floor(eye.x + Mth.cos(angle) * distance), Mth.floor(eye.z + Mth.sin(angle) * distance));
			if (perch == null || birds.stream().anyMatch(bird -> bird.perch.at.distanceToSqr(perch.at) < kind.apart * kind.apart)) {
				continue;
			}
			Bird bird = arriving(level, perch, eye);
			if (bird != null) {
				birds.add(bird);
				return;
			}
		}
	}

	/**
	 * Coming in to land from beyond its perch, as seen from the player: the way it would take off, flown backwards, so it
	 * comes in fast, slows and drops onto it. Null when each way tried runs into a block.
	 */
	private @Nullable Bird arriving(ClientLevel level, Perch perch, Vec3 eye) {
		float beyond = (float) Mth.atan2(perch.at.z - eye.z, perch.at.x - eye.x);
		for (int i = 0; i < WAYS_TRIED; i++) {
			Vec3[] path = takeOffPath(beyond + Mth.randomBetween(RANDOM, -0.8F, 0.8F));
			if (clear(level, perch.at, path)) {
				return new Bird(level, perch, path);
			}
		}
		return null;
	}

	/** Where it would be tick by tick, from its perch, flying off along this bearing. */
	private static Vec3[] takeOffPath(float bearing) {
		Vec3[] path = new Vec3[LANDING + 1];
		path[0] = Vec3.ZERO;
		Vec3 velocity = takeOffVelocity(bearing);
		for (int tick = 1; tick <= LANDING; tick++) {
			path[tick] = path[tick - 1].add(velocity);
			velocity = flapped(velocity, tick);
		}
		return path;
	}

	/** Nothing but air on its way in, tick by tick, past the first ticks over its own perch. */
	private static boolean clear(ClientLevel level, Vec3 perch, Vec3[] path) {
		for (int tick = 2; tick <= LANDING; tick++) {
			if (!open(level, perch.add(path[tick]))) {
				return false;
			}
		}
		return true;
	}

	/** Air where its body is, from its feet to its back. */
	private static boolean open(ClientLevel level, Vec3 at) {
		return level.getBlockState(BlockPos.containing(at.add(0, 0.1, 0))).isAir() && level.getBlockState(BlockPos.containing(at.add(0, 0.3, 0))).isAir();
	}

	/**
	 * Its way off from here along this bearing, tick by tick, around the blocks: where its climb would take it into one, it
	 * flies on level, under a roof's eaves until it is out; where its way ahead is shut, it turns. Null when it is shut in.
	 */
	private static Vec3 @Nullable [] avoidingPath(ClientLevel level, Vec3 from, float bearing) {
		Vec3[] path = new Vec3[FLIGHT + 1];
		path[0] = Vec3.ZERO;
		Vec3 velocity = takeOffVelocity(bearing);
		for (int tick = 1; tick <= FLIGHT; tick++) {
			Vec3 step = null;
			for (Vec3 tried : around(velocity)) {
				if (open(level, from.add(path[tick - 1]).add(tried))) {
					step = tried;
					break;
				}
			}
			if (step == null) {
				return null;
			}
			path[tick] = path[tick - 1].add(step);
			velocity = flapped(step, tick);
		}
		return path;
	}

	/** The step it means to take, then the same flown level, then level and turned further and further aside. */
	private static Vec3[] around(Vec3 velocity) {
		Vec3 level = new Vec3(velocity.x, 0, velocity.z);
		if (level.lengthSqr() < 1.0E-4) {
			level = new Vec3(0.08, 0, 0);
		}
		Vec3[] steps = new Vec3[2 + 2 * AVOID_TURNS.length];
		steps[0] = velocity;
		steps[1] = level;
		for (int i = 0; i < AVOID_TURNS.length; i++) {
			steps[2 + 2 * i] = level.yRot(AVOID_TURNS[i]);
			steps[3 + 2 * i] = level.yRot(-AVOID_TURNS[i]);
		}
		return steps;
	}

	/** Rising off its perch before it goes off level. */
	private static Vec3 takeOffVelocity(float bearing) {
		return new Vec3(Mth.cos(bearing) * 0.08, 0.22, Mth.sin(bearing) * 0.08);
	}

	private static Vec3 flapped(Vec3 velocity, int flight) {
		double ahead = velocity.horizontalDistance();
		double speed = Math.min(ahead + 0.03, TOP_SPEED) / ahead;
		// Its climb slows as it gets clear, and its way rises and dips as it beats then closes its wings
		double climb = Math.max(velocity.y * 0.88, 0.02) + Mth.sin(flight * 0.35F) * 0.03;
		return new Vec3(velocity.x * speed, climb, velocity.z * speed);
	}

	void draw(LevelRenderContext context) {
		if (birds.isEmpty()) {
			return;
		}
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 camera = context.levelState().cameraRenderState.pos;
		RenderType type = RenderTypes.entityCutout(kind.texture);
		for (Bird bird : birds) {
			bird.draw(context, type, camera, partialTick);
		}
	}

	/** Who comes near: the player, or anyone else around. */
	private static boolean disturbed(ClientLevel level, Vec3 at) {
		for (Player player : level.players()) {
			float reach = player.isShiftKeyDown() ? FLEE_SNEAKING : player.isSprinting() ? FLEE_RUNNING : FLEE;
			if (!player.isSpectator() && player.position().distanceToSqr(at) < reach * reach) {
				return true;
			}
		}
		return false;
	}

	private final class Bird {
		private static final int HOP = 6;
		private static final float HOP_HEIGHT = 0.15F;
		private static final int FLICK = 5;

		final ClientLevel level;
		final Perch perch;
		final int stay = Mth.randomBetweenInclusive(RANDOM, MIN_STAY, MAX_STAY);
		final float phase = RANDOM.nextFloat() * Mth.TWO_PI;
		// Its own model: the parts' angles are read when the frame is drawn, after every bird was submitted
		final ModelPart root = MODEL.bakeRoot();
		final ModelPart head = root.getChild("head");
		final ModelPart tail = root.getChild("tail");
		final ModelPart leftWing = root.getChild("left_wing");
		final ModelPart rightWing = root.getChild("right_wing");
		int age;
		float yaw = RANDOM.nextFloat() * Mth.TWO_PI;
		float headYaw;
		/** Where it stands on its perch, moved by its hops. */
		float x;
		float z;
		float hopFromX;
		float hopFromZ;
		float hopFromYaw;
		float hopToYaw;
		int hop = -1;
		int flick = -1;
		int song = Mth.randomBetweenInclusive(RANDOM, kind.minSongWait / 2, kind.maxSongWait);
		int singing;
		int next = Mth.randomBetweenInclusive(RANDOM, 20, 80);
		/** Ticks since it took off, -1 while perched. */
		int flight = -1;
		/** Where it took off from, and its way off from there, worked out around the blocks as it took off. */
		Vec3 flightStart = Vec3.ZERO;
		Vec3[] flightPath = new Vec3[0];
		Vec3 flown = Vec3.ZERO;
		Vec3 flownBefore = Vec3.ZERO;
		Vec3 velocity = Vec3.ZERO;
		/** Its way in from its perch, flown from the far end: the step of it where it is, -1 once landed. */
		final Vec3[] path;
		int landing = -1;

		Bird(ClientLevel level, Perch perch) {
			this(level, perch, new Vec3[0]);
		}

		Bird(ClientLevel level, Perch perch, Vec3[] path) {
			this.level = level;
			this.perch = perch;
			this.path = path;
			if (path.length > 1) {
				landing = path.length - 1;
				velocity = path[landing - 1].subtract(path[landing]);
			}
		}

		void tick(Minecraft minecraft, boolean stay) {
			age++;
			if (flight >= 0) {
				fly();
				return;
			}
			Vec3 at = position(0);
			if (landing >= 0) {
				// Someone already by its perch: it turns away and goes off again
				if (disturbed(level, perch.at)) {
					landing = -1;
					takeOff(minecraft, at);
				} else if (--landing >= 0) {
					velocity = path[landing].subtract(path[landing + 1]);
				} else {
					AmbientSound.of(SoundEvents.PARROT_FLY.location(), LANDING_VOLUME).from(perch.at).fading(TAKEOFF_RANGE)
							.pitch(kind.takeOffPitch + Mth.randomBetween(RANDOM, 0.1F, 0.3F)).play(minecraft);
				}
				return;
			}
			if (disturbed(level, at) || !kind.perches.holds(level, perch) || age > this.stay || !stay && RANDOM.nextInt(LEAVE_ODDS) == 0) {
				takeOff(minecraft, at);
				return;
			}
			if (hop >= 0 && ++hop > HOP) {
				hop = -1;
			}
			if (flick >= 0 && ++flick > FLICK) {
				flick = -1;
			}
			if (singing > 0) {
				singing--;
			} else if (--song <= 0) {
				song = Mth.randomBetweenInclusive(RANDOM, kind.minSongWait, kind.maxSongWait);
				singing = kind.singing;
				headYaw = 0;
				AmbientSound.of(kind.song, kind.songVolume).from(at.add(0, 0.3, 0)).fading(kind.songRange).muffled().pitch(Mth.randomBetween(RANDOM, 0.95F, 1.05F))
						.play(minecraft);
			}
			if (--next > 0 || singing > 0) {
				return;
			}
			next = Mth.randomBetweenInclusive(RANDOM, 15, 70);
			// Restless, as small birds are: a hop to face elsewhere, a flick of the tail, a look around
			switch (RANDOM.nextInt(4)) {
				case 0 -> {
					hop = 0;
					hopFromX = x;
					hopFromZ = z;
					hopFromYaw = yaw;
					hopToYaw = yaw + Mth.randomBetween(RANDOM, 0.6F, 2.2F) * (RANDOM.nextBoolean() ? 1 : -1);
					x = Mth.clamp(x + Mth.randomBetween(RANDOM, -0.15F, 0.15F), -perch.roam, perch.roam);
					z = Mth.clamp(z + Mth.randomBetween(RANDOM, -0.15F, 0.15F), -perch.roam, perch.roam);
					yaw = hopToYaw;
					headYaw = 0;
				}
				case 1 -> flick = 0;
				default -> headYaw = Mth.randomBetween(RANDOM, -0.9F, 0.9F);
			}
		}

		/**
		 * Away from whoever is nearest, rising off its perch before it goes off level, its flight bounding a little: its whole way
		 * worked out now, around the blocks (avoidingPath), from the bearing straight away first, then turning further from it.
		 */
		private void takeOff(Minecraft minecraft, Vec3 at) {
			flight = 0;
			Player nearest = level.getNearestPlayer(at.x, at.y, at.z, GONE, false);
			float away = nearest == null ? RANDOM.nextFloat() * Mth.TWO_PI
					: (float) Mth.atan2(at.z - nearest.getZ(), at.x - nearest.getX()) + Mth.randomBetween(RANDOM, -0.6F, 0.6F);
			flightStart = at;
			flightPath = null;
			for (float turn : TURNS) {
				flightPath = avoidingPath(level, at, away + turn);
				if (flightPath != null) {
					break;
				}
			}
			if (flightPath == null) {
				// Shut in on every side: it goes off straight, as nothing could be seen of it anyway
				flightPath = new Vec3[FLIGHT + 1];
				Vec3[] straight = takeOffPath(away);
				for (int tick = 0; tick <= FLIGHT; tick++) {
					flightPath[tick] = straight[Math.min(tick, LANDING)];
				}
			}
			flown = flownBefore = Vec3.ZERO;
			velocity = flightPath[1];
			AmbientSound.of(SoundEvents.PARROT_FLY.location(), TAKEOFF_VOLUME).from(at).fading(TAKEOFF_RANGE)
					.pitch(kind.takeOffPitch + Mth.randomBetween(RANDOM, 0, 0.2F)).play(minecraft);
		}

		private void fly() {
			flight++;
			flownBefore = flown;
			flown = flightPath[Math.min(flight, flightPath.length - 1)];
			velocity = flown.subtract(flownBefore);
		}

		Vec3 position(float partialTick) {
			Vec3 at = perch.at.add(x, 0, z);
			if (landing >= 0) {
				return perch.at.add(path[Math.min(landing + 1, path.length - 1)].lerp(path[landing], partialTick));
			}
			if (flight >= 0) {
				return flightStart.add(flownBefore.lerp(flown, partialTick));
			}
			if (hop >= 0) {
				float done = Mth.clamp((hop + partialTick) / HOP, 0, 1);
				return perch.at.add(Mth.lerp(done, hopFromX, x), done * (1 - done) * 4 * HOP_HEIGHT, Mth.lerp(done, hopFromZ, z));
			}
			return at;
		}

		void draw(LevelRenderContext context, RenderType type, Vec3 camera, float partialTick) {
			float time = age + partialTick;
			float size = kind.scale;
			float facing = yaw;
			float pitch = 0;
			if (flight >= 0 || landing >= 0) {
				float flying = flight >= 0 ? flight + partialTick : LANDING - landing + partialTick;
				size *= flight >= 0 ? Mth.clamp((FLIGHT - flying) / FADE, 0, 1) : Mth.clamp(flying / FADE, 0, 1);
				facing = (float) Mth.atan2(velocity.x, velocity.z);
				pitch = (float) -Mth.atan2(velocity.y, velocity.horizontalDistance()) * 0.7F;
				// Fast beats, the wings out to the side
				float beat = Mth.sin(flying * 1.9F + phase) * 0.9F;
				leftWing.zRot = -Mth.HALF_PI + beat;
				rightWing.zRot = Mth.HALF_PI - beat;
				head.yRot = 0;
				head.xRot = 0;
				tail.xRot = -0.1F;
			} else {
				if (hop >= 0) {
					facing = Mth.rotLerp(Mth.clamp((hop + partialTick) / HOP, 0, 1), hopFromYaw * Mth.RAD_TO_DEG, hopToYaw * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
				}
				leftWing.zRot = 0;
				rightWing.zRot = 0;
				head.yRot = headYaw;
				// Its head up to sing, a quiver of the throat
				head.xRot = singing > 0 ? 0.5F + Mth.sin(time * 2.5F) * 0.05F : 0;
				float flicking = flick >= 0 ? Mth.sin((flick + partialTick) / FLICK * Mth.PI) : 0;
				tail.xRot = -0.35F - flicking * 0.6F;
			}
			if (size <= 0) {
				return;
			}
			Vec3 at = position(partialTick);
			BlockPos block = BlockPos.containing(at.add(0, 0.2, 0));
			int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, block), level.getBrightness(LightLayer.SKY, block));
			PoseStack pose = context.poseStack();
			pose.pushPose();
			pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
			pose.rotate(Axis.YP.rotation(facing));
			pose.rotate(Axis.XP.rotation(pitch));
			pose.scale(-size, -size, size);
			context.submitNodeCollector().submitModelPart(root, pose, type, light, OverlayTexture.NO_OVERLAY, null);
			pose.popPose();
		}
	}

	/**
	 * One pixel a sixteenth of a block as for any mob, its head towards +Z, standing on y = 0: a round body, the head above its
	 * front, a tail cocked up behind, the wings folded along its sides, swung out to fly.
	 */
	private static LayerDefinition model() {
		MeshDefinition mesh = new MeshDefinition();
		// The head sits into the front of the body, as wide: a hair wider, so their sides do not flicker against each other
		CubeDeformation headOverBody = new CubeDeformation(0.1F);
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -3, -2, 3, 3, 4), PartPose.ZERO);
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -3, -1.5F, 3, 3, 3, headOverBody), PartPose.offset(0, -2, 1.5F));
		head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(12, 8).addBox(-0.5F, -1.5F, 1.5F, 1, 1, 1), PartPose.ZERO);
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 14).addBox(-1, 0, -3, 2, 1, 3), PartPose.offset(0, -2, -2));
		root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(16, 14).addBox(0, 0, -3, 1, 3, 4), PartPose.offset(1.5F, -3, 0.5F));
		root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(16, 14).mirror().addBox(-1, 0, -3, 1, 3, 4), PartPose.offset(-1.5F, -3, 0.5F));
		return LayerDefinition.create(mesh, 32, 32);
	}
}
