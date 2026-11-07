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
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;
import com.cassonade.weather.SandLayerBlock;

/**
 * Now and then by day in a desert or the badlands, a scorpion comes up out of the sand near the player and wanders about.
 * Anyone coming close finds it rearing up to face them, claws raised and tail cocked; closer still, it jabs its sting at the
 * air and digs back into the sand. A model and nothing more, as the squirrels: no entity, nothing to touch, no harm done,
 * and each player sees their own.
 */
public final class Scorpions {
	private static final Identifier TEXTURE = Cassonade.id("textures/entity/scorpion.png");
	private static final int MAX = 1;
	private static final int LOOK_EVERY = 40;
	/** The chance, each time it looks, that one comes: about once in half a minute, where nobody is about. */
	private static final int COME_ODDS = 15;
	private static final int COLUMNS_TRIED = 12;
	/** Out of reach of the player, and close enough to be made out. */
	private static final float NEAREST = 6;
	private static final float FARTHEST = 16;
	private static final float APART = 8;
	private static final float GONE = 48;
	/** How far from where it came up it wanders. */
	private static final float WANDER = 2.5F;
	/** How near anyone may come before it rears up, closer for one sneaking; and before it strikes. */
	private static final float THREAT = 2;
	private static final float THREAT_SNEAKING = 1.3F;
	private static final float STRIKE = 1;
	/** How long it stands its ground before digging in, if nobody comes closer nor goes away. */
	private static final int WARN_FOR = 80;
	private static final int MIN_STAY = 1200;
	private static final int MAX_STAY = 3600;
	/** Each tick at dusk or in a sandstorm, the chance it leaves: they go one by one rather than all at once. */
	private static final int LEAVE_ODDS = 100;
	private static final float SCALE = 0.75F;
	/** How far under the sand it is drawn before it comes up: the tip of its tail too. */
	private static final float BURIED = 0.6F;
	private static final int RISE = 20;
	private static final int SINK = 16;
	private static final int STRIKE_TICKS = 8;
	/** Blocks a tick, and how fast it turns towards where it goes or whom it faces. */
	private static final float WALK = 0.035F;
	private static final float TURN = 0.25F;
	/** Its way over the sand is looked at every so often, and this far each side of its middle. */
	private static final float WAY_STEP = 0.1F;
	private static final float WAY_WIDTH = 0.2F;
	/** The most it climbs or drops from one spot to the next, a drift of sand four layers deep. */
	private static final float STEP = 0.5F;
	private static final float SAND_VOLUME = 0.4F;
	private static final float HISS_VOLUME = 0.25F;
	private static final float SOUND_RANGE = 12;
	private static final LayerDefinition MODEL = model();
	private static final RandomSource RANDOM = RandomSource.create();
	private static final List<Scorpion> SCORPIONS = new ArrayList<>();
	private static boolean spawning = true;
	private static int look;

	private Scorpions() {
	}

	public static int count() {
		return SCORPIONS.size();
	}

	/** How many stand facing someone, claws raised; for tests. */
	public static long rearing() {
		return SCORPIONS.stream().filter(scorpion -> scorpion.state == State.WARNING).count();
	}

	/** How many are out of the sand and about; for tests. */
	public static long roaming() {
		return SCORPIONS.stream().filter(scorpion -> scorpion.state == State.ROAMING).count();
	}

	/** How many have struck or are digging back in; for tests. */
	public static long leaving() {
		return SCORPIONS.stream().filter(scorpion -> scorpion.state == State.STRIKING || scorpion.state == State.SINKING).count();
	}

	/** Where the first one is; for tests. */
	public static @Nullable Vec3 first() {
		return SCORPIONS.isEmpty() ? null : SCORPIONS.getFirst().position(0);
	}

	/** For tests: stops scorpions coming on their own and sends the others away at once, or lets them come again. */
	public static void setSpawning(boolean spawning) {
		Scorpions.spawning = spawning;
		SCORPIONS.clear();
	}

	/** For tests: one coming up out of the sand at this column; false where the ground is not sand open to the sky. */
	public static boolean comeUp(Minecraft minecraft, int x, int z) {
		Vec3 spot = sand(minecraft.level, x, z);
		if (spot != null) {
			SCORPIONS.add(new Scorpion(minecraft.level, spot));
		}
		return spot != null;
	}

	/** One coming up out of the sand before the player's eyes, to watch it in runClient; false with no sand in sight. */
	static boolean comeUpInView(Minecraft minecraft) {
		Vec3 eye = minecraft.player.getEyePosition();
		Vec3 look = minecraft.player.getLookAngle();
		float bearing = (float) Mth.atan2(look.z, look.x);
		for (int distance = 4; distance <= 24; distance++) {
			for (float turn = 0; turn <= 0.4F; turn += 0.05F) {
				for (int side = -1; side <= 1; side += 2) {
					float angle = bearing + turn * side;
					if (comeUp(minecraft, Mth.floor(eye.x + Mth.cos(angle) * distance), Mth.floor(eye.z + Mth.sin(angle) * distance))) {
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
			SCORPIONS.clear();
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		Vec3 player = minecraft.player.position();
		boolean stay = Ambience.daytime(level) && !level.isRaining();
		SCORPIONS.removeIf(scorpion -> scorpion.level != level || scorpion.gone || scorpion.home.distanceToSqr(player) > GONE * GONE);
		for (Scorpion scorpion : SCORPIONS) {
			scorpion.tick(minecraft, stay);
		}
		if (++look < LOOK_EVERY) {
			return;
		}
		look = 0;
		if (spawning && stay && SCORPIONS.size() < MAX && RANDOM.nextInt(COME_ODDS) == 0 && Ambience.overworld(level) && level.getBrightness(LightLayer.SKY, minecraft.player.blockPosition()) > 0
				&& dry(level.getBiome(minecraft.player.blockPosition()))) {
			settle(level, player);
		}
	}

	private static boolean dry(Holder<Biome> biome) {
		return biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS);
	}

	/** Up out of the sand near the player, in sight or not, away from the others and from anyone. */
	private static void settle(ClientLevel level, Vec3 player) {
		for (int i = 0; i < COLUMNS_TRIED; i++) {
			float angle = RANDOM.nextFloat() * Mth.TWO_PI;
			float distance = Mth.randomBetween(RANDOM, NEAREST, FARTHEST);
			Vec3 spot = sand(level, Mth.floor(player.x + Mth.cos(angle) * distance), Mth.floor(player.z + Mth.sin(angle) * distance));
			if (spot != null && SCORPIONS.stream().noneMatch(scorpion -> scorpion.home.distanceToSqr(spot) < APART * APART)
					&& nearest(level, spot) > NEAREST) {
				SCORPIONS.add(new Scorpion(level, spot));
				return;
			}
		}
	}

	/** On the sand at the top of this column, open to the sky; null elsewhere. */
	private static @Nullable Vec3 sand(ClientLevel level, int x, int z) {
		double y = surface(level, x, z, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
		return Double.isNaN(y) ? null : new Vec3(x + 0.5, y, z + 0.5);
	}

	/**
	 * The height it stands at on this column, on sand or on the layers a sandstorm drifts over it, looked for from the block
	 * above this one down to the block below; NaN where there is no sand, or no room over it.
	 */
	private static double surface(ClientLevel level, int x, int z, int y) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int at = y + 1; at >= y - 1; at--) {
			BlockState state = level.getBlockState(pos.set(x, at, z));
			if (state.getBlock() instanceof SandLayerBlock) {
				return open(level, pos.move(0, 1, 0)) ? at + state.getShape(level, pos.move(0, -1, 0)).max(Direction.Axis.Y) : Double.NaN;
			}
			if (state.is(BlockTags.SAND)) {
				return open(level, pos.move(0, 1, 0)) ? at + 1 : Double.NaN;
			}
		}
		return Double.NaN;
	}

	/** Room for a scorpion: nothing solid, no water, where a dead bush or dry grass is fine, but not a layer of sand. */
	private static boolean open(ClientLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty() && !(state.getBlock() instanceof SandLayerBlock);
	}

	/** The height it stands at here, from the height it stood at. */
	private static double surface(ClientLevel level, double x, double z, double y) {
		return surface(level, Mth.floor(x), Mth.floor(z), Mth.floor(y - 0.01));
	}

	/** How far the nearest player is, spectators unseen. */
	private static double nearest(ClientLevel level, Vec3 at) {
		double nearest = Double.MAX_VALUE;
		for (Player player : level.players()) {
			if (!player.isSpectator()) {
				nearest = Math.min(nearest, player.position().distanceTo(at));
			}
		}
		return nearest;
	}

	static void draw(LevelRenderContext context) {
		if (SCORPIONS.isEmpty()) {
			return;
		}
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 camera = context.levelState().cameraRenderState.pos;
		RenderType type = RenderTypes.entityCutout(TEXTURE);
		for (Scorpion scorpion : SCORPIONS) {
			scorpion.draw(context, type, camera, partialTick);
		}
	}

	private enum State {
		RISING, ROAMING, WARNING, STRIKING, SINKING
	}

	private static final class Scorpion {
		private static final int LEGS = 4;
		/** The pace of its legs: a full cycle every so many blocks walked. */
		private static final float STRIDE = 0.2F;

		final ClientLevel level;
		final Vec3 home;
		final int stay = Mth.randomBetweenInclusive(RANDOM, MIN_STAY, MAX_STAY);
		// Its own model: the parts' angles are read when the frame is drawn, after every scorpion was submitted
		final ModelPart root = MODEL.bakeRoot();
		final ModelPart body = root.getChild("body");
		final ModelPart[] leftLegs = new ModelPart[LEGS];
		final ModelPart[] rightLegs = new ModelPart[LEGS];
		final ModelPart leftArm = body.getChild("left_arm");
		final ModelPart rightArm = body.getChild("right_arm");
		final ModelPart leftFinger = leftArm.getChild("finger");
		final ModelPart rightFinger = rightArm.getChild("finger");
		final ModelPart[] tail = new ModelPart[5];
		State state = State.RISING;
		int stateTick;
		int age;
		boolean gone;
		Vec3 at;
		Vec3 atBefore;
		float yaw = RANDOM.nextFloat() * Mth.TWO_PI;
		float yawBefore = yaw;
		@Nullable Vec3 goal;
		int pause = Mth.randomBetweenInclusive(RANDOM, 10, 40);
		float walked;
		float walkedBefore;
		/** How far it has reared up, 0 to 1, and how far under the sand it is. */
		float threat;
		float threatBefore;
		float sunk = 1;
		float sunkBefore = 1;

		Scorpion(ClientLevel level, Vec3 home) {
			this.level = level;
			this.home = at = atBefore = home;
			for (int i = 0; i < LEGS; i++) {
				leftLegs[i] = body.getChild("left_leg_" + i);
				rightLegs[i] = body.getChild("right_leg_" + i);
			}
			ModelPart part = body;
			for (int i = 0; i < tail.length; i++) {
				part = part.getChild(i < tail.length - 1 ? "tail_" + i : "sting");
				tail[i] = part;
			}
		}

		void tick(Minecraft minecraft, boolean calm) {
			if (age++ == 0) {
				dig(minecraft);
			}
			atBefore = at;
			yawBefore = yaw;
			walkedBefore = walked;
			threatBefore = threat;
			sunkBefore = sunk;
			threat = Mth.approach(threat, state == State.WARNING || state == State.STRIKING ? 1 : 0, 0.15F);
			stateTick++;
			Player near = nearestPlayer();
			double distance = near == null ? Double.MAX_VALUE : near.position().distanceTo(at);
			float reach = near != null && near.isShiftKeyDown() ? THREAT_SNEAKING : THREAT;
			switch (state) {
				case RISING -> {
					sunk = 1 - Math.min(1, (float) stateTick / RISE);
					if (stateTick % 4 == 0) {
						puff();
					}
					if (stateTick >= RISE) {
						enter(State.ROAMING);
					}
				}
				case ROAMING -> {
					if (distance < reach) {
						enter(State.WARNING);
						goal = null;
						hiss(minecraft);
					} else if (age > stay || !calm && RANDOM.nextInt(LEAVE_ODDS) == 0) {
						enter(State.SINKING);
						dig(minecraft);
					} else {
						wander();
					}
				}
				case WARNING -> {
					face(near == null ? yaw : (float) Mth.atan2(near.getX() - at.x, near.getZ() - at.z));
					if (distance < STRIKE) {
						enter(State.STRIKING);
					} else if (distance > reach + 1) {
						enter(State.ROAMING);
						pause = Mth.randomBetweenInclusive(RANDOM, 20, 60);
					} else if (stateTick > WARN_FOR) {
						enter(State.SINKING);
						dig(minecraft);
					}
				}
				case STRIKING -> {
					if (stateTick >= STRIKE_TICKS) {
						enter(State.SINKING);
						dig(minecraft);
					}
				}
				case SINKING -> {
					sunk = Math.min(1, (float) stateTick / SINK);
					if (stateTick % 3 == 0) {
						puff();
					}
					gone = stateTick >= SINK;
				}
			}
		}

		private void enter(State next) {
			state = next;
			stateTick = 0;
		}

		/** A few steps to a spot near where it came up, a stop, another few steps: slow and deliberate, as scorpions go. */
		private void wander() {
			if (goal == null) {
				if (pause-- > 0) {
					return;
				}
				for (int i = 0; i < 4 && goal == null; i++) {
					float angle = RANDOM.nextFloat() * Mth.TWO_PI;
					float distance = RANDOM.nextFloat() * WANDER;
					Vec3 spot = home.add(Mth.cos(angle) * distance, 0, Mth.sin(angle) * distance);
					if (spot.distanceTo(at) > 0.5 && clearWay(at, spot)) {
						goal = spot;
					}
				}
				pause = Mth.randomBetweenInclusive(RANDOM, 20, 100);
				return;
			}
			Vec3 to = goal.subtract(at);
			double length = to.horizontalDistance();
			if (length < WALK) {
				double y = surface(level, goal.x, goal.z, at.y);
				at = new Vec3(goal.x, Double.isNaN(y) ? at.y : y, goal.z);
				goal = null;
				return;
			}
			float heading = (float) Mth.atan2(to.x, to.z);
			face(heading);
			// It turns on the spot before it sets off
			if (Math.abs(Mth.wrapDegrees((heading - yaw) * Mth.RAD_TO_DEG)) < 30) {
				Vec3 next = at.add(to.scale(WALK / length));
				double y = surface(level, next.x, next.z, at.y);
				at = new Vec3(next.x, Double.isNaN(y) ? at.y : y, next.z);
				walked += WALK;
			}
		}

		private void face(float heading) {
			float turn = Mth.wrapDegrees((heading - yaw) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
			yaw += Mth.clamp(turn, -TURN, TURN);
		}

		/** Sand all along from one spot to the other, as wide as its body, over drifts no higher than it can step. */
		private boolean clearWay(Vec3 from, Vec3 to) {
			int samples = Math.max(1, Mth.ceil(Math.hypot(to.x - from.x, to.z - from.z) / WAY_STEP));
			double y = from.y;
			for (int i = 0; i <= samples; i++) {
				Vec3 spot = from.lerp(to, (double) i / samples);
				for (int corner = 0; corner < 4; corner++) {
					double height = surface(level, spot.x + ((corner & 1) == 0 ? -WAY_WIDTH : WAY_WIDTH), spot.z + ((corner & 2) == 0 ? -WAY_WIDTH : WAY_WIDTH), y);
					if (Double.isNaN(height) || Math.abs(height - y) > STEP) {
						return false;
					}
				}
				y = surface(level, spot.x, spot.z, y);
				if (Double.isNaN(y)) {
					return false;
				}
			}
			return true;
		}

		private @Nullable Player nearestPlayer() {
			Player nearest = null;
			for (Player player : level.players()) {
				if (!player.isSpectator() && (nearest == null || player.distanceToSqr(at) < nearest.distanceToSqr(at))) {
					nearest = player;
				}
			}
			return nearest;
		}

		private void puff() {
			BlockPos under = BlockPos.containing(at.x, at.y - 0.01, at.z);
			BlockParticleOption sand = new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(under));
			for (int i = 0; i < 2; i++) {
				level.addParticle(sand, at.x + Mth.randomBetween(RANDOM, -0.25F, 0.25F), at.y + 0.05, at.z + Mth.randomBetween(RANDOM, -0.25F, 0.25F), 0, 0.05, 0);
			}
		}

		private void dig(Minecraft minecraft) {
			AmbientSound.of(SoundEvents.BRUSH_SAND.location(), SAND_VOLUME).from(at).fading(SOUND_RANGE).pitch(Mth.randomBetween(RANDOM, 1.1F, 1.4F)).play(minecraft);
		}

		/** A dry rattle as it rears up. */
		private void hiss(Minecraft minecraft) {
			AmbientSound.of(SoundEvents.SILVERFISH_AMBIENT.location(), HISS_VOLUME).from(at).fading(SOUND_RANGE).pitch(Mth.randomBetween(RANDOM, 1.5F, 1.8F)).play(minecraft);
		}

		Vec3 position(float partialTick) {
			return atBefore.lerp(at, partialTick);
		}

		void draw(LevelRenderContext context, RenderType type, Vec3 camera, float partialTick) {
			float time = age + partialTick;
			float threat = Mth.lerp(partialTick, threatBefore, this.threat);
			float walking = Mth.lerp(partialTick, walkedBefore, walked) / STRIDE * Mth.TWO_PI;
			boolean moving = walked != walkedBefore;
			float strike = state == State.STRIKING ? Mth.sin(Math.min(1, (stateTick + partialTick) / STRIKE_TICKS) * Mth.PI) : 0;

			// Its front lifted as it rears up
			body.xRot = threat * 0.2F;
			for (int i = 0; i < LEGS; i++) {
				// Fanned out, front legs forward and hind legs back, alternate legs stepping together
				float splay = (1.5F - i) * 0.35F;
				float step = moving ? Mth.sin(walking + (i % 2) * Mth.PI) * 0.3F : 0;
				float lift = moving ? Math.max(0, Mth.cos(walking + (i % 2) * Mth.PI)) * 0.2F : 0;
				leftLegs[i].yRot = splay + step;
				rightLegs[i].yRot = -splay + step;
				leftLegs[i].zRot = 0.75F - lift;
				rightLegs[i].zRot = -0.75F + lift;
			}
			// Claws held out low, raised and opening and closing as it faces someone
			float clack = threat * (0.35F + Mth.sin(time * 1.2F) * 0.2F);
			leftArm.xRot = rightArm.xRot = threat * 0.5F;
			leftArm.yRot = 0.45F + threat * 0.25F;
			rightArm.yRot = -leftArm.yRot;
			leftFinger.yRot = -clack;
			rightFinger.yRot = clack;
			// The tail curled over its back, cocked higher as it rears, and swung forward to strike
			float sway = Mth.sin(time * 0.08F) * 0.05F;
			tail[0].xRot = -0.6F - threat * 0.3F + sway;
			tail[1].xRot = -0.6F + sway;
			tail[2].xRot = -0.6F - strike * 0.5F;
			tail[3].xRot = -0.6F - strike * 0.5F;
			tail[4].xRot = -0.6F + threat * 0.2F - strike * 0.6F;

			Vec3 position = position(partialTick);
			BlockPos block = BlockPos.containing(position.add(0, 0.2, 0));
			int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, block), level.getBrightness(LightLayer.SKY, block));
			float sunk = Mth.lerp(partialTick, sunkBefore, this.sunk);
			PoseStack pose = context.poseStack();
			pose.pushPose();
			pose.translate(position.x - camera.x, position.y - sunk * BURIED - camera.y, position.z - camera.z);
			pose.rotate(Axis.YP.rotation(Mth.rotLerp(partialTick, yawBefore * Mth.RAD_TO_DEG, yaw * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD));
			pose.scale(-SCALE, -SCALE, SCALE);
			context.submitNodeCollector().submitModelPart(root, pose, type, light, OverlayTexture.NO_OVERLAY, null);
			pose.popPose();
		}
	}

	/**
	 * One pixel a sixteenth of a block as for any mob, its head towards +Z, standing on y = 0: a flat body on four legs each
	 * side, two claws reaching forward, and a tail of four segments and a sting, curled up over its back.
	 */
	private static LayerDefinition model() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2, -2, -3, 4, 2, 6), PartPose.offset(0, -1, 0));
		for (int i = 0; i < 4; i++) {
			float z = -2 + i * 1.3F;
			body.addOrReplaceChild("left_leg_" + i, CubeListBuilder.create().texOffs(20, 0).addBox(0, -0.5F, -0.5F, 3, 1, 1), PartPose.offset(1.9F, -1, z));
			body.addOrReplaceChild("right_leg_" + i, CubeListBuilder.create().texOffs(20, 0).mirror().addBox(-3, -0.5F, -0.5F, 3, 1, 1), PartPose.offset(-1.9F, -1, z));
		}
		// Each claw a palm and a finger beside it on the inner side, which opens
		PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(20, 2).addBox(-0.5F, -0.5F, 0, 1, 1, 3)
				.texOffs(0, 8).addBox(-0.5F, -0.5F, 3, 2, 1, 2), PartPose.offset(1.4F, -0.8F, 2.6F));
		leftArm.addOrReplaceChild("finger", CubeListBuilder.create().texOffs(8, 8).addBox(-1, -0.5F, 0, 1, 1, 2), PartPose.offset(-0.5F, 0, 3));
		PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(20, 2).mirror().addBox(-0.5F, -0.5F, 0, 1, 1, 3)
				.texOffs(0, 8).mirror().addBox(-1.5F, -0.5F, 3, 2, 1, 2), PartPose.offset(-1.4F, -0.8F, 2.6F));
		rightArm.addOrReplaceChild("finger", CubeListBuilder.create().texOffs(8, 8).mirror().addBox(0, -0.5F, 0, 1, 1, 2), PartPose.offset(0.5F, 0, 3));
		// Every other segment a hair wider, so their sides, in the same planes, do not flicker against each other where they meet
		PartDefinition part = body;
		float back = -3;
		for (int i = 0; i < 4; i++) {
			CubeDeformation width = new CubeDeformation(i % 2 == 0 ? 0.05F : 0);
			part = part.addOrReplaceChild("tail_" + i, CubeListBuilder.create().texOffs(0, 11).addBox(-1, -1, -2, 2, 2, 2, width),
					PartPose.offset(0, i == 0 ? -1.2F : 0, back));
			back = -1.8F;
		}
		part.addOrReplaceChild("sting", CubeListBuilder.create().texOffs(8, 11).addBox(-0.5F, -0.5F, -2, 1, 1, 2), PartPose.offset(0, 0, back));
		return LayerDefinition.create(mesh, 32, 32);
	}
}
