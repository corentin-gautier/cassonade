package com.cassonade.client.ambience;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;

/**
 * Now and then by day, a robin perched on the top of a tree near the player, where they are heard singing: it hops about,
 * looks around, sings from where it stands, and flies off as anyone comes near, closer for one sneaking up on it. A model and
 * nothing more, as the butterflies: no entity, nothing to touch, and each player sees their own.
 */
public final class Robins {
	private static final Identifier TEXTURE = Cassonade.id("textures/entity/robin.png");
	private static final int MAX = 3;
	private static final int LOOK_EVERY = 40;
	private static final int COLUMNS_TRIED = 12;
	/** Settled out of reach of the player, and close enough to be made out. */
	private static final float NEAREST = 9;
	private static final float FARTHEST = 22;
	/** Robins keep apart, each on its own tree. */
	private static final float APART = 6;
	/** Out of the view, so none appears before the eyes: the cosine of the angle from where the player looks. */
	private static final float OUT_OF_VIEW = 0.2F;
	/** How near anyone may come before it flies off: farther for one running, closer for one sneaking. */
	private static final float FLEE = 6;
	private static final float FLEE_RUNNING = 9;
	private static final float FLEE_SNEAKING = 3;
	private static final float GONE = 64;
	private static final int MIN_STAY = 1200;
	private static final int MAX_STAY = 4800;
	/** Each tick at dusk or in the rain, the chance it leaves: they go one by one rather than all at once. */
	private static final int LEAVE_ODDS = 100;
	private static final float SCALE = 0.8F;
	private static final float SONG_VOLUME = 0.3F;
	private static final float SONG_RANGE = 48;
	private static final int MIN_SONG_WAIT = 160;
	private static final int MAX_SONG_WAIT = 600;
	private static final int SINGING = 50;
	private static final float TAKEOFF_VOLUME = 0.5F;
	private static final float TAKEOFF_RANGE = 20;
	private static final int FLIGHT = 90;
	/** Shrinks away at the end of its flight, far off by then. */
	private static final int FADE = 15;
	private static final float TOP_SPEED = 0.45F;
	private static final LayerDefinition MODEL = model();
	private static final RandomSource RANDOM = RandomSource.create();
	private static final List<Robin> ROBINS = new ArrayList<>();
	private static boolean spawning = true;
	private static int look;

	private Robins() {
	}

	public static int count() {
		return ROBINS.size();
	}

	/** How many have flown off and are still in the air; for tests. */
	public static long flying() {
		return ROBINS.stream().filter(robin -> robin.flight >= 0).count();
	}

	/** Where the first one is; for tests. */
	public static @Nullable Vec3 first() {
		return ROBINS.isEmpty() ? null : ROBINS.getFirst().position(0);
	}

	/** For tests: stops robins coming on their own and sends the others away at once, or lets them come again. */
	public static void setSpawning(boolean spawning) {
		Robins.spawning = spawning;
		ROBINS.clear();
	}

	/** For tests: one on this leaf block, facing the player. */
	public static void perch(Minecraft minecraft, BlockPos leaves) {
		Robin robin = new Robin(minecraft.level, Vec3.atBottomCenterOf(leaves.above()));
		Vec3 eye = minecraft.player.getEyePosition();
		robin.yaw = (float) Mth.atan2(eye.x - robin.perch.x, eye.z - robin.perch.z);
		ROBINS.add(robin);
	}

	/** One on the top of a tree before the player's eyes, to watch it in runClient; false with none in sight. */
	static boolean perchInView(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		Vec3 eye = minecraft.player.getEyePosition();
		Vec3 look = minecraft.player.getLookAngle();
		float bearing = (float) Mth.atan2(look.z, look.x);
		for (int distance = 5; distance <= 32; distance++) {
			for (float turn = 0; turn <= 0.5F; turn += 0.05F) {
				for (int side = -1; side <= 1; side += 2) {
					float angle = bearing + turn * side;
					BlockPos leaves = treeTop(level, Mth.floor(eye.x + Mth.cos(angle) * distance), Mth.floor(eye.z + Mth.sin(angle) * distance));
					if (leaves != null) {
						perch(minecraft, leaves);
						return true;
					}
				}
			}
		}
		return false;
	}

	static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null) {
			ROBINS.clear();
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		Vec3 player = minecraft.player.position();
		boolean stay = Ambience.daytime(level) && !level.isRaining();
		ROBINS.removeIf(robin -> robin.level != level || robin.perch.distanceToSqr(player) > GONE * GONE || robin.flight >= FLIGHT);
		for (Robin robin : ROBINS) {
			robin.tick(minecraft, stay);
		}
		if (++look < LOOK_EVERY) {
			return;
		}
		look = 0;
		if (spawning && ROBINS.size() < MAX && Ambience.overworld(level) && level.getBrightness(LightLayer.SKY, minecraft.player.blockPosition()) > 0) {
			settle(minecraft, level);
		}
	}

	/** Where robins sing, on a tree top out of sight. */
	private static void settle(Minecraft minecraft, ClientLevel level) {
		Identifier songs = AirSounds.birdsAt(level, minecraft.player.blockPosition());
		if (songs != AirSounds.MEADOW && songs != AirSounds.FOREST_DAY) {
			return;
		}
		Vec3 eye = minecraft.player.getEyePosition();
		Vec3 look = minecraft.player.getLookAngle();
		for (int i = 0; i < COLUMNS_TRIED; i++) {
			float angle = RANDOM.nextFloat() * Mth.TWO_PI;
			float distance = Mth.randomBetween(RANDOM, NEAREST, FARTHEST);
			BlockPos leaves = treeTop(level, Mth.floor(eye.x + Mth.cos(angle) * distance), Mth.floor(eye.z + Mth.sin(angle) * distance));
			if (leaves == null) {
				continue;
			}
			Vec3 perch = Vec3.atBottomCenterOf(leaves.above());
			if (perch.subtract(eye).normalize().dot(look) < OUT_OF_VIEW && ROBINS.stream().noneMatch(robin -> robin.perch.distanceToSqr(perch) < APART * APART)) {
				ROBINS.add(new Robin(level, perch));
				return;
			}
		}
	}

	/** The column's highest block, if it is leaves: open sky above, where a robin sings. */
	private static @Nullable BlockPos treeTop(ClientLevel level, int x, int z) {
		BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
		return level.getBlockState(top).is(BlockTags.LEAVES) ? top : null;
	}

	static void draw(LevelRenderContext context) {
		if (ROBINS.isEmpty()) {
			return;
		}
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 camera = context.levelState().cameraRenderState.pos;
		RenderType type = RenderTypes.entityCutout(TEXTURE);
		for (Robin robin : ROBINS) {
			robin.draw(context, type, camera, partialTick);
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

	private static final class Robin {
		private static final int HOP = 6;
		private static final float HOP_HEIGHT = 0.15F;
		private static final int FLICK = 5;
		/** Within the top of its leaf block, so a hop never takes it over the edge. */
		private static final float ROAM = 0.3F;

		final ClientLevel level;
		final Vec3 perch;
		final int stay = Mth.randomBetweenInclusive(RANDOM, MIN_STAY, MAX_STAY);
		final float phase = RANDOM.nextFloat() * Mth.TWO_PI;
		// Its own model: the parts' angles are read when the frame is drawn, after every robin was submitted
		final ModelPart root = MODEL.bakeRoot();
		final ModelPart head = root.getChild("head");
		final ModelPart tail = root.getChild("tail");
		final ModelPart leftWing = root.getChild("left_wing");
		final ModelPart rightWing = root.getChild("right_wing");
		int age;
		float yaw = RANDOM.nextFloat() * Mth.TWO_PI;
		float headYaw;
		/** Where it stands on its leaves, moved by its hops. */
		float x;
		float z;
		float hopFromX;
		float hopFromZ;
		float hopFromYaw;
		float hopToYaw;
		int hop = -1;
		int flick = -1;
		int song = Mth.randomBetweenInclusive(RANDOM, MIN_SONG_WAIT / 2, MAX_SONG_WAIT);
		int singing;
		int next = Mth.randomBetweenInclusive(RANDOM, 20, 80);
		/** Ticks since it took off, -1 while perched. */
		int flight = -1;
		Vec3 flown = Vec3.ZERO;
		Vec3 flownBefore = Vec3.ZERO;
		Vec3 velocity = Vec3.ZERO;

		Robin(ClientLevel level, Vec3 perch) {
			this.level = level;
			this.perch = perch;
		}

		void tick(Minecraft minecraft, boolean stay) {
			age++;
			if (flight >= 0) {
				fly();
				return;
			}
			Vec3 at = position(0);
			if (disturbed(level, at) || !level.getBlockState(BlockPos.containing(perch).below()).is(BlockTags.LEAVES)
					|| age > this.stay || !stay && RANDOM.nextInt(LEAVE_ODDS) == 0) {
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
				song = Mth.randomBetweenInclusive(RANDOM, MIN_SONG_WAIT, MAX_SONG_WAIT);
				singing = SINGING;
				headYaw = 0;
				AmbientSound.of(AirSounds.MEADOW, SONG_VOLUME).from(at.add(0, 0.3, 0)).fading(SONG_RANGE).muffled().pitch(Mth.randomBetween(RANDOM, 0.95F, 1.05F)).play(minecraft);
			}
			if (--next > 0 || singing > 0) {
				return;
			}
			next = Mth.randomBetweenInclusive(RANDOM, 15, 70);
			// Restless, as robins are: a hop to face elsewhere, a flick of the tail, a look around
			switch (RANDOM.nextInt(4)) {
				case 0 -> {
					hop = 0;
					hopFromX = x;
					hopFromZ = z;
					hopFromYaw = yaw;
					hopToYaw = yaw + Mth.randomBetween(RANDOM, 0.6F, 2.2F) * (RANDOM.nextBoolean() ? 1 : -1);
					x = Mth.clamp(x + Mth.randomBetween(RANDOM, -0.15F, 0.15F), -ROAM, ROAM);
					z = Mth.clamp(z + Mth.randomBetween(RANDOM, -0.15F, 0.15F), -ROAM, ROAM);
					yaw = hopToYaw;
					headYaw = 0;
				}
				case 1 -> flick = 0;
				default -> headYaw = Mth.randomBetween(RANDOM, -0.9F, 0.9F);
			}
		}

		/** Away from whoever is nearest, rising out of the tree before it goes off level, its flight bounding a little. */
		private void takeOff(Minecraft minecraft, Vec3 at) {
			flight = 0;
			Player nearest = level.getNearestPlayer(at.x, at.y, at.z, GONE, false);
			float away = nearest == null ? RANDOM.nextFloat() * Mth.TWO_PI
					: (float) Mth.atan2(at.z - nearest.getZ(), at.x - nearest.getX()) + Mth.randomBetween(RANDOM, -0.6F, 0.6F);
			velocity = new Vec3(Mth.cos(away) * 0.08, 0.22, Mth.sin(away) * 0.08);
			AmbientSound.of(SoundEvents.PARROT_FLY.location(), TAKEOFF_VOLUME).from(at).fading(TAKEOFF_RANGE).pitch(Mth.randomBetween(RANDOM, 1.3F, 1.5F)).play(minecraft);
		}

		private void fly() {
			flight++;
			flownBefore = flown;
			flown = flown.add(velocity);
			double ahead = velocity.horizontalDistance();
			double speed = Math.min(ahead + 0.03, TOP_SPEED) / ahead;
			// Its climb slows as it gets clear of the leaves, and its way rises and dips as it beats then closes its wings
			double climb = Math.max(velocity.y * 0.88, 0.02) + Mth.sin(flight * 0.35F) * 0.03;
			velocity = new Vec3(velocity.x * speed, climb, velocity.z * speed);
		}

		Vec3 position(float partialTick) {
			Vec3 at = perch.add(x, 0, z);
			if (flight >= 0) {
				return at.add(flownBefore.lerp(flown, partialTick));
			}
			if (hop >= 0) {
				float done = Mth.clamp((hop + partialTick) / HOP, 0, 1);
				return perch.add(Mth.lerp(done, hopFromX, x), done * (1 - done) * 4 * HOP_HEIGHT, Mth.lerp(done, hopFromZ, z));
			}
			return at;
		}

		void draw(LevelRenderContext context, RenderType type, Vec3 camera, float partialTick) {
			float time = age + partialTick;
			float size = SCALE;
			float facing = yaw;
			float pitch = 0;
			if (flight >= 0) {
				float flying = flight + partialTick;
				size *= Mth.clamp((FLIGHT - flying) / FADE, 0, 1);
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
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -3, -2, 3, 3, 4), PartPose.ZERO);
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -3, -1.5F, 3, 3, 3), PartPose.offset(0, -2, 1.5F));
		head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(12, 8).addBox(-0.5F, -1.5F, 1.5F, 1, 1, 1), PartPose.ZERO);
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 14).addBox(-1, 0, -3, 2, 1, 3), PartPose.offset(0, -2, -2));
		root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(16, 14).addBox(0, 0, -3, 1, 3, 4), PartPose.offset(1.5F, -3, 0.5F));
		root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(16, 14).mirror().addBox(-1, 0, -3, 1, 3, 4), PartPose.offset(-1.5F, -3, 0.5F));
		return LayerDefinition.create(mesh, 32, 32);
	}
}
