package com.cassonade.client.ambience;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;

/**
 * Now and then by day in a forest or a taiga, a red squirrel comes down a tree trunk out of the leaves near the player: it
 * stops on the bark or goes down to the foot of the tree, sits up to nibble, noses about, bounds a little, and runs back up
 * into the leaves as anyone comes near, closer for one sneaking up on it. A model and nothing more, as the robins: no entity,
 * nothing to touch, and each player sees their own.
 */
public final class Squirrels {
	private static final Identifier TEXTURE = Cassonade.id("textures/entity/squirrel.png");
	private static final int MAX = 2;
	private static final int LOOK_EVERY = 40;
	private static final int COLUMNS_TRIED = 12;
	/** Out of reach of the player, and close enough to be made out. */
	private static final float NEAREST = 8;
	private static final float FARTHEST = 20;
	/** Each on its own tree, well apart. */
	private static final float APART = 10;
	/** How far below a tree top and around it its trunk is looked for. */
	private static final int CANOPY_DEPTH = 4;
	private static final int CANOPY_REACH = 2;
	/** The bare trunk it comes down, from the ground to the first leaves beside it. */
	private static final int MIN_BARE = 2;
	private static final int MAX_BARE = 16;
	/** How near anyone may come before it runs off: farther for one running, closer for one sneaking. */
	private static final float FLEE = 7;
	private static final float FLEE_RUNNING = 10;
	private static final float FLEE_SNEAKING = 3.5F;
	private static final float GONE = 64;
	private static final int MIN_STAY = 1200;
	private static final int MAX_STAY = 3600;
	/** Each tick at dusk or in the rain, the chance it leaves: they go one by one rather than all at once. */
	private static final int LEAVE_ODDS = 100;
	private static final float SCALE = 0.8F;
	/** Blocks a tick up or down the bark, calm or running off, and over the ground. */
	private static final float CLIMB = 0.12F;
	private static final float DASH = 0.3F;
	private static final float BOUND = 0.12F;
	/** Out of sight once this far into the leaves. */
	private static final float HIDDEN = 0.7F;
	/** Where it stands on the bark at the foot of the tree, head up or head down: its body clear of the ground. */
	private static final float FOOT_UP = 0.25F;
	private static final float FOOT_DOWN = 0.35F;
	/** How far from the trunk it wanders at its foot. */
	private static final float NEAR_FOOT = 0.8F;
	/** Its way over the ground is looked at every so often, and this far each side of its middle. */
	private static final float WAY_STEP = 0.1F;
	private static final float WAY_WIDTH = 0.15F;
	private static final float FAR_FOOT = 1.8F;
	private static final float ALONG_FOOT = 1;
	private static final float RUSTLE_VOLUME = 0.35F;
	private static final float RUSTLE_RANGE = 16;
	private static final LayerDefinition MODEL = model();
	private static final RandomSource RANDOM = RandomSource.create();
	private static final List<Squirrel> SQUIRRELS = new ArrayList<>();
	private static boolean spawning = true;
	private static int look;

	private Squirrels() {
	}

	public static int count() {
		return SQUIRRELS.size();
	}

	/** How many are running back up into the leaves; for tests. */
	public static long fleeing() {
		return SQUIRRELS.stream().filter(squirrel -> squirrel.leaving).count();
	}

	/** How many have come down and settled, on the bark or at the foot of the tree; for tests. */
	public static long settled() {
		return SQUIRRELS.stream().filter(squirrel -> squirrel.settled && !squirrel.leaving).count();
	}

	/** Where the first one is; for tests. */
	public static @Nullable Vec3 first() {
		return SQUIRRELS.isEmpty() ? null : SQUIRRELS.getFirst().position(0);
	}

	/** For tests: those on the ground sit up to nibble, for a while. */
	public static void sitUp() {
		for (Squirrel squirrel : SQUIRRELS) {
			if (squirrel.settled && squirrel.step == null && !squirrel.onBark()) {
				squirrel.sitting = true;
				squirrel.sit = squirrel.sitBefore = 1;
				squirrel.pause = 400;
			}
		}
	}

	/** For tests: stops squirrels coming on their own and sends the others away at once, or lets them come again. */
	public static void setSpawning(boolean spawning) {
		Squirrels.spawning = spawning;
		SQUIRRELS.clear();
	}

	/**
	 * For tests: one coming down the trunk this log belongs to, going down to its foot or stopping on the bark; false
	 * without a bare trunk under leaves.
	 */
	public static boolean comeDown(Minecraft minecraft, BlockPos log, boolean toGround) {
		Tree tree = tree(minecraft.level, log);
		if (tree != null) {
			SQUIRRELS.add(new Squirrel(minecraft.level, tree, toGround));
		}
		return tree != null;
	}

	/** One coming down a tree before the player's eyes, to watch it in runClient; false with none in sight. */
	static boolean comeDownInView(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		Vec3 eye = minecraft.player.getEyePosition();
		Vec3 look = minecraft.player.getLookAngle();
		float bearing = (float) Mth.atan2(look.z, look.x);
		for (int distance = 5; distance <= 32; distance++) {
			for (float turn = 0; turn <= 0.5F; turn += 0.05F) {
				for (int side = -1; side <= 1; side += 2) {
					float angle = bearing + turn * side;
					Tree tree = treeUnder(level, Mth.floor(eye.x + Mth.cos(angle) * distance), Mth.floor(eye.z + Mth.sin(angle) * distance));
					if (tree != null) {
						SQUIRRELS.add(new Squirrel(level, tree, RANDOM.nextInt(3) > 0));
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
			SQUIRRELS.clear();
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		Vec3 player = minecraft.player.position();
		boolean stay = Ambience.daytime(level) && !level.isRaining();
		SQUIRRELS.removeIf(squirrel -> squirrel.level != level || squirrel.gone || squirrel.tree.center().distanceToSqr(player) > GONE * GONE
				|| !level.getBlockState(squirrel.tree.foot).is(BlockTags.LOGS));
		for (Squirrel squirrel : SQUIRRELS) {
			squirrel.tick(minecraft, stay);
		}
		if (++look < LOOK_EVERY) {
			return;
		}
		look = 0;
		if (spawning && stay && SQUIRRELS.size() < MAX && Ambience.overworld(level) && level.getBrightness(LightLayer.SKY, minecraft.player.blockPosition()) > 0
				&& woods(level.getBiome(minecraft.player.blockPosition()))) {
			settle(level, player);
		}
	}

	/** Forests and taigas, where red squirrels live, but not the jungle. */
	private static boolean woods(Holder<Biome> biome) {
		return (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA)) && !biome.is(BiomeTags.IS_JUNGLE);
	}

	/** Down a trunk near the player, in sight or not, where nobody stands yet. */
	private static void settle(ClientLevel level, Vec3 player) {
		for (int i = 0; i < COLUMNS_TRIED; i++) {
			float angle = RANDOM.nextFloat() * Mth.TWO_PI;
			float distance = Mth.randomBetween(RANDOM, NEAREST, FARTHEST);
			Tree tree = treeUnder(level, Mth.floor(player.x + Mth.cos(angle) * distance), Mth.floor(player.z + Mth.sin(angle) * distance));
			if (tree == null || tree.center().distanceToSqr(player) < NEAREST * NEAREST) {
				continue;
			}
			Vec3 foot = tree.center();
			if (SQUIRRELS.stream().noneMatch(squirrel -> squirrel.tree.center().distanceToSqr(foot) < APART * APART)
					&& !disturbed(level, tree.side(tree.low + FOOT_DOWN))) {
				SQUIRRELS.add(new Squirrel(level, tree, RANDOM.nextInt(3) > 0));
				return;
			}
		}
	}

	/** The trunk under this column's top, if its top is leaves: looked for a few blocks down and around. */
	private static @Nullable Tree treeUnder(ClientLevel level, int x, int z) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
		if (!level.getBlockState(new BlockPos(x, top, z)).is(BlockTags.LEAVES)) {
			return null;
		}
		for (int down = 1; down <= CANOPY_DEPTH; down++) {
			for (int dx = -CANOPY_REACH; dx <= CANOPY_REACH; dx++) {
				for (int dz = -CANOPY_REACH; dz <= CANOPY_REACH; dz++) {
					BlockPos at = new BlockPos(x + dx, top - down, z + dz);
					if (level.getBlockState(at).is(BlockTags.LOGS)) {
						return tree(level, at);
					}
				}
			}
		}
		return null;
	}

	/**
	 * The trunk this log belongs to, with a side bare from the ground up to leaves, which hide the squirrel as it comes and
	 * goes; null with none. A wide trunk's inner sides touch each other and are left out.
	 */
	private static @Nullable Tree tree(ClientLevel level, BlockPos log) {
		BlockPos foot = log;
		for (int i = 0; i < MAX_BARE * 2 && level.getBlockState(foot.below()).is(BlockTags.LOGS); i++) {
			foot = foot.below();
		}
		if (!level.getBlockState(foot).is(BlockTags.LOGS) || !ground(level, foot.below())) {
			return null;
		}
		int first = RANDOM.nextInt(4);
		for (int i = 0; i < 4; i++) {
			Direction face = Direction.from2DDataValue((first + i) % 4);
			BlockPos side = foot.relative(face);
			if (!ground(level, side.below())) {
				continue;
			}
			int bare = 0;
			while (bare < MAX_BARE && open(level, side.above(bare)) && level.getBlockState(foot.above(bare)).is(BlockTags.LOGS)) {
				bare++;
			}
			if (bare >= MIN_BARE && level.getBlockState(side.above(bare)).is(BlockTags.LEAVES) && level.getBlockState(foot.above(bare)).is(BlockTags.LOGS)) {
				return new Tree(foot, face, foot.getY(), foot.getY() + bare);
			}
		}
		return null;
	}

	private static boolean ground(ClientLevel level, BlockPos pos) {
		return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
	}

	/** Room for a squirrel: nothing solid, no water, no leaves, where grass and flowers are fine. */
	private static boolean open(ClientLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES);
	}

	static void draw(LevelRenderContext context) {
		if (SQUIRRELS.isEmpty()) {
			return;
		}
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 camera = context.levelState().cameraRenderState.pos;
		RenderType type = RenderTypes.entityCutout(TEXTURE);
		for (Squirrel squirrel : SQUIRRELS) {
			squirrel.draw(context, type, camera, partialTick);
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

	/**
	 * A trunk's bare side, from its foot (the ground's height) up to where leaves start beside it.
	 */
	private record Tree(BlockPos foot, Direction face, double low, double high) {
		Vec3 center() {
			return Vec3.atCenterOf(foot);
		}

		/** On the bark at this height, the belly against it. */
		Vec3 side(double y) {
			return new Vec3(foot.getX() + 0.5 + face.getStepX() * 0.5, y, foot.getZ() + 0.5 + face.getStepZ() * 0.5);
		}

		/** On the ground in front of the bark. */
		Vec3 front(float out) {
			return side(low).add(face.getStepX() * out, 0, face.getStepZ() * out);
		}

		/** Facing away from the trunk, the way a squirrel faces coming down it head first. */
		float outward() {
			return (float) Mth.atan2(face.getStepX(), face.getStepZ());
		}
	}

	private enum Gait {
		STILL, SCURRY, HOP
	}

	/** Where it goes next, in so many ticks, then how long it waits there. */
	private record Step(Vec3 to, float pitch, float yaw, int ticks, int pause, Gait gait, boolean last) {
	}

	private static final class Squirrel {
		private static final int FLICK = 10;
		/** Its body on the bark: its head down, or up. */
		private static final float HEAD_DOWN = Mth.HALF_PI;
		private static final float HEAD_UP = -Mth.HALF_PI;
		private static final float STRIDE = 0.3F;
		private static final float HOP_HEIGHT = 0.12F;
		private static final float SIT = 1.1F;

		final ClientLevel level;
		final Tree tree;
		final int stay = Mth.randomBetweenInclusive(RANDOM, MIN_STAY, MAX_STAY);
		// Its own model: the parts' angles are read when the frame is drawn, after every squirrel was submitted
		final ModelPart root = MODEL.bakeRoot();
		final ModelPart body = root.getChild("body");
		final ModelPart head = body.getChild("head");
		final ModelPart tail = body.getChild("tail");
		final ModelPart tailTip = tail.getChild("tip");
		final ModelPart leftArm = body.getChild("left_arm");
		final ModelPart rightArm = body.getChild("right_arm");
		final ModelPart leftLeg = body.getChild("left_leg");
		final ModelPart rightLeg = body.getChild("right_leg");
		final Deque<Step> steps = new ArrayDeque<>();
		int age;
		/** The step it is on, null while it stands; where it comes from, and where the step takes it (where it stands). */
		@Nullable Step step;
		int stepTick;
		int strides = 1;
		Vec3 from;
		float fromPitch;
		float fromYaw;
		Vec3 at;
		float pitch;
		float yaw;
		/** Where the steps still to take leave it. */
		Vec3 end;
		float endPitch;
		float endYaw;
		int pause;
		boolean settled;
		boolean leaving;
		boolean gone;
		boolean sitting;
		boolean foraging;
		float sit;
		float sitBefore;
		float headYaw;
		float headYawBefore;
		float headTarget;
		int flick = -1;

		/** Head first out of the leaves, down to the foot of its tree or to a stop on the bark. */
		Squirrel(ClientLevel level, Tree tree, boolean toGround) {
			this.level = level;
			this.tree = tree;
			from = at = end = tree.side(tree.high + HIDDEN);
			fromPitch = pitch = endPitch = HEAD_DOWN;
			fromYaw = yaw = endYaw = tree.outward();
			double lowest = tree.low + 1;
			double highest = tree.high - 0.4;
			if (toGround || highest <= lowest) {
				down(tree.low + FOOT_DOWN, CLIMB);
				onGround();
			} else {
				down(Mth.lerp(RANDOM.nextFloat(), lowest, highest), CLIMB);
			}
		}

		/** Down the bark, head first, in bursts as squirrels go: a dash, a stop to look around, another dash. */
		private void down(double goal, float speed) {
			while (end.y > goal + 0.01) {
				double length = Math.min(Mth.randomBetween(RANDOM, 0.4F, 1.3F), end.y - goal);
				queue(tree.side(end.y - length), HEAD_DOWN, tree.outward(), length / speed, Mth.randomBetweenInclusive(RANDOM, 4, 30), Gait.SCURRY, false);
			}
		}

		/** From the foot of the bark, head down, off onto the ground. */
		private void onGround() {
			queue(tree.front(0.3F), 0, tree.outward(), 5, 0, Gait.STILL, false);
			queue(foot(), 0, endYaw + Mth.randomBetween(RANDOM, -0.8F, 0.8F), 0, Mth.randomBetweenInclusive(RANDOM, 10, 40), Gait.HOP, false);
		}

		/** From the ground onto the bark, head up. */
		private void onBark(int ticks) {
			Vec3 front = tree.front(0.3F);
			float back = tree.outward() + Mth.PI;
			queue(front, 0, back, front.distanceTo(end) / (leaving ? DASH : BOUND), 0, Gait.HOP, false);
			queue(tree.side(tree.low + FOOT_UP), HEAD_UP, back, ticks, 0, Gait.STILL, false);
		}

		/**
		 * A spot near the foot of its tree, the way there and back to the bark clear, so it never bounds through a block nor
		 * over a hole, nor gets cut off from its tree; the one right before the bark if none is, or where it stands.
		 */
		private Vec3 foot() {
			Direction along = tree.face.getClockWise();
			Vec3 bark = tree.front(0.3F);
			for (int i = 0; i < 4; i++) {
				float out = Mth.randomBetween(RANDOM, NEAR_FOOT, FAR_FOOT);
				float aside = Mth.randomBetween(RANDOM, -ALONG_FOOT, ALONG_FOOT);
				Vec3 spot = tree.front(out).add(along.getStepX() * aside, 0, along.getStepZ() * aside);
				if (clearWay(end, spot) && clearWay(spot, bark)) {
					return spot;
				}
			}
			Vec3 near = tree.front(NEAR_FOOT);
			return clearWay(end, near) ? near : end;
		}

		/** Room all along the ground from one spot to the other, as wide as its body. */
		private boolean clearWay(Vec3 from, Vec3 to) {
			int samples = Math.max(1, Mth.ceil(from.distanceTo(to) / WAY_STEP));
			for (int i = 0; i <= samples; i++) {
				Vec3 at = from.lerp(to, (double) i / samples);
				for (int corner = 0; corner < 4; corner++) {
					BlockPos block = BlockPos.containing(at.x + ((corner & 1) == 0 ? -WAY_WIDTH : WAY_WIDTH), tree.low + 0.05,
							at.z + ((corner & 2) == 0 ? -WAY_WIDTH : WAY_WIDTH));
					if (!open(level, block) || !ground(level, block.below())) {
						return false;
					}
				}
			}
			return true;
		}

		/** Only where the steps already queued leave it: the first starts from where it stands. */
		private void queue(Vec3 to, float pitch, float yaw, double ticks, int pause, Gait gait, boolean last) {
			int length = gait == Gait.HOP && ticks <= 0 ? Math.max(4, Mth.ceil(to.distanceTo(end) / BOUND)) : Math.max(1, Mth.ceil(ticks));
			steps.add(new Step(to, pitch, yaw, length, pause, gait, last));
			end = to;
			endPitch = pitch;
			endYaw = yaw;
		}

		boolean onBark() {
			return Math.abs(endPitch) > 0.1F;
		}

		void tick(Minecraft minecraft, boolean calm) {
			if (age++ == 0) {
				rustle(minecraft);
			}
			sitBefore = sit;
			sit = Mth.approach(sit, sitting ? 1 : 0, 0.2F);
			headYawBefore = headYaw;
			headYaw += (headTarget - headYaw) * 0.4F;
			if (flick >= 0 && ++flick > FLICK) {
				flick = -1;
			}
			if (!leaving && disturbed(level, position(0))) {
				leave(true);
			} else if (!leaving && settled && (age > stay || !calm && RANDOM.nextInt(LEAVE_ODDS) == 0)) {
				leave(false);
			}
			if (step != null) {
				if (++stepTick < step.ticks()) {
					return;
				}
				pause = step.pause();
				if (step.last()) {
					gone = true;
					rustle(minecraft);
				}
				step = null;
			}
			if (pause-- > 0) {
				return;
			}
			sitting = foraging = false;
			Step next = steps.poll();
			if (next != null) {
				begin(next);
			} else if (!leaving) {
				settled = true;
				idle();
			}
		}

		private void begin(Step next) {
			step = next;
			stepTick = 0;
			from = at;
			fromPitch = pitch;
			fromYaw = yaw;
			at = next.to();
			pitch = next.pitch();
			yaw = next.yaw();
			strides = Math.max(1, Math.round((float) from.distanceTo(at) / STRIDE));
			headTarget = 0;
		}

		/** Back up into the leaves, at once and fast when someone comes near, at its own pace otherwise. */
		private void leave(boolean fleeing) {
			// Stops where it is, mid-step
			Vec3 here = position(0);
			from = at = end = here;
			fromPitch = pitch = endPitch = pitch(0);
			fromYaw = yaw = endYaw = yaw(0);
			step = null;
			steps.clear();
			pause = 0;
			sitting = foraging = false;
			leaving = true;
			if (fleeing) {
				flick = 0;
			}
			float speed = fleeing ? DASH : CLIMB;
			float back = tree.outward() + Mth.PI;
			if (!onBark()) {
				onBark(fleeing ? 3 : 6);
			} else if (endPitch > 0) {
				queue(end, HEAD_UP, back, fleeing ? 3 : 8, 0, Gait.STILL, false);
			}
			double top = tree.high + HIDDEN;
			while (end.y < top - 0.01) {
				double length = fleeing ? top - end.y : Math.min(Mth.randomBetween(RANDOM, 0.6F, 1.6F), top - end.y);
				boolean last = end.y + length >= top - 0.01;
				queue(tree.side(end.y + length), HEAD_UP, back, length / speed, fleeing ? 0 : Mth.randomBetweenInclusive(RANDOM, 4, 20), Gait.SCURRY, last);
			}
		}

		/** Restless, as squirrels are: a look around, a flick of the tail, a dash, and at the foot of its tree, a meal. */
		private void idle() {
			if (onBark()) {
				switch (RANDOM.nextInt(7)) {
					case 0, 1 -> look();
					case 2 -> twitch();
					case 3, 4 -> {
						double dash = Mth.randomBetween(RANDOM, 0.25F, 0.8F) * (pitch > 0 ? -1 : 1);
						double y = Mth.clamp(at.y + dash, tree.low + 1, tree.high - 0.4);
						if (Math.abs(y - at.y) > 0.1) {
							queue(tree.side(y), pitch, yaw, Math.abs(y - at.y) / CLIMB, Mth.randomBetweenInclusive(RANDOM, 10, 40), Gait.SCURRY, false);
						} else {
							turn();
						}
					}
					case 5 -> turn();
					default -> {
						if (pitch < 0) {
							turn();
						}
						down(tree.low + FOOT_DOWN, CLIMB);
						onGround();
					}
				}
				return;
			}
			switch (RANDOM.nextInt(8)) {
				case 0, 1 -> {
					sitting = true;
					pause = Mth.randomBetweenInclusive(RANDOM, 60, 200);
				}
				case 2 -> {
					foraging = true;
					pause = Mth.randomBetweenInclusive(RANDOM, 30, 90);
				}
				case 3 -> look();
				case 4 -> twitch();
				case 5, 6 -> {
					Vec3 spot = foot();
					if (spot.distanceToSqr(at) < 0.01) {
						// Nowhere clear to go: it stays and looks around
						look();
					} else {
						queue(spot, 0, (float) Mth.atan2(spot.x - at.x, spot.z - at.z), 0, Mth.randomBetweenInclusive(RANDOM, 10, 40), Gait.HOP, false);
					}
				}
				default -> {
					// A little way up the bark, to come down again later
					onBark(6);
					double y = Math.min(end.y + Mth.randomBetween(RANDOM, 0.6F, 1.5F), tree.high - 0.4);
					queue(tree.side(y), HEAD_UP, endYaw, (y - end.y) / CLIMB, Mth.randomBetweenInclusive(RANDOM, 10, 40), Gait.SCURRY, false);
				}
			}
		}

		private void look() {
			headTarget = Mth.randomBetween(RANDOM, -0.9F, 0.9F);
			pause = Mth.randomBetweenInclusive(RANDOM, 10, 40);
		}

		private void twitch() {
			flick = 0;
			pause = Mth.randomBetweenInclusive(RANDOM, 10, 30);
		}

		/** Head up to head down on the bark, or the other way. */
		private void turn() {
			boolean up = pitch > 0;
			queue(at, up ? HEAD_UP : HEAD_DOWN, tree.outward() + (up ? Mth.PI : 0), 7, Mth.randomBetweenInclusive(RANDOM, 10, 30), Gait.STILL, false);
		}

		private void rustle(Minecraft minecraft) {
			AmbientSound.of(SoundEvents.AZALEA_LEAVES_STEP.location(), RUSTLE_VOLUME).from(tree.side(tree.high + 0.5)).fading(RUSTLE_RANGE)
					.pitch(Mth.randomBetween(RANDOM, 1.1F, 1.3F)).play(minecraft);
		}

		private float progress(float partialTick) {
			return step == null ? 1 : Mth.clamp((stepTick + partialTick) / step.ticks(), 0, 1);
		}

		Vec3 position(float partialTick) {
			return from.lerp(at, progress(partialTick));
		}

		float pitch(float partialTick) {
			return Mth.lerp(progress(partialTick), fromPitch, pitch);
		}

		/** Turned within its first bounds over the ground, all along a turn on the bark. */
		float yaw(float partialTick) {
			float turned = step != null && step.gait() == Gait.HOP ? Math.min(1, progress(partialTick) * 3) : progress(partialTick);
			return Mth.rotLerp(turned, fromYaw * Mth.RAD_TO_DEG, yaw * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
		}

		void draw(LevelRenderContext context, RenderType type, Vec3 camera, float partialTick) {
			float time = age + partialTick;
			float bodyPitch = pitch(partialTick);
			float clinging = Math.min(1, Math.abs(bodyPitch) / Mth.HALF_PI);
			float sitting = Mth.lerp(partialTick, sitBefore, sit);
			// Where it is in its strides: bounds over the ground, scurrying legs on the bark
			float stride = 0;
			Gait gait = step == null ? Gait.STILL : step.gait();
			if (gait != Gait.STILL) {
				float strided = progress(partialTick) * strides;
				stride = strided - Mth.floor(strided);
				if (progress(partialTick) >= 1) {
					stride = 0;
				}
			}
			float swing = Mth.sin(stride * Mth.TWO_PI);
			float rise = gait == Gait.HOP ? stride * (1 - stride) * 4 * HOP_HEIGHT : 0;
			float flicking = flick >= 0 ? Mth.sin((flick + partialTick) / FLICK * Mth.TWO_PI * 2) * 0.5F : 0;

			body.xRot = sitting * SIT + (gait == Gait.HOP ? -swing * 0.25F : 0);
			head.yRot = Mth.lerp(partialTick, headYawBefore, headYaw);
			if (foraging) {
				head.xRot = -0.6F + Mth.sin(time * 1.3F) * 0.15F;
			} else if (sitting > 0) {
				// Upright, its head level, nibbling what it holds in its forepaws
				head.xRot = -sitting * SIT * 0.85F + Mth.sin(time * 1.7F) * 0.08F * sitting;
			} else {
				// On the bark, its head lifted off it to look out
				head.xRot = clinging * 0.35F;
			}
			float paws = foraging ? Mth.sin(time * 1.3F) * 0.4F : sitting * (1.2F + Mth.sin(time * 1.7F) * 0.12F);
			float legs = gait == Gait.STILL ? 0 : swing * (gait == Gait.HOP ? 0.8F : 0.6F);
			leftArm.xRot = paws + legs;
			rightArm.xRot = (foraging ? -paws : paws) + legs;
			leftLeg.xRot = rightLeg.xRot = -sitting * SIT - legs;
			// Its tail up behind its back in an S, its tip curled back, lying along the bark as it climbs
			tail.xRot = Mth.lerp(clinging, -1.6F, -0.25F) - sitting * SIT + flicking + (gait == Gait.SCURRY ? swing * 0.1F : 0);
			tailTip.xRot = Mth.lerp(clinging, 0.7F, 0.15F) + flicking * 0.6F;

			Vec3 position = position(partialTick);
			Vec3 out = new Vec3(tree.face.getStepX(), 0, tree.face.getStepZ());
			BlockPos block = BlockPos.containing(position.add(out.scale(0.2)).add(0, 0.2, 0));
			int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, block), level.getBrightness(LightLayer.SKY, block));
			// Off the bark by a hair, so its paws do not flicker against it
			Vec3 drawn = position.add(out.scale(clinging * 0.01)).add(0, rise, 0);
			PoseStack pose = context.poseStack();
			pose.pushPose();
			pose.translate(drawn.x - camera.x, drawn.y - camera.y, drawn.z - camera.z);
			pose.rotate(Axis.YP.rotation(yaw(partialTick)));
			pose.rotate(Axis.XP.rotation(bodyPitch));
			pose.scale(-SCALE, -SCALE, SCALE);
			context.submitNodeCollector().submitModelPart(root, pose, type, light, OverlayTexture.NO_OVERLAY, null);
			pose.popPose();
		}
	}

	/**
	 * One pixel a sixteenth of a block as for any mob, its head towards +Z, standing on y = 0: a long body turning about its
	 * haunches to sit up, the head at its front with tufted ears, the forepaws under its chest, and a bushy tail in two parts.
	 */
	private static LayerDefinition model() {
		MeshDefinition mesh = new MeshDefinition();
		// The head sits into the front of the body, as wide: a hair wider, so their sides do not flicker against each other;
		// the legs, under its sides, front and back, a hair inside them for the same reason
		CubeDeformation headOverBody = new CubeDeformation(0.1F);
		float limbs = 0.9F;
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -3, 0, 3, 3, 5), PartPose.offset(0, -1, -2));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -1.5F, -0.5F, 3, 3, 3, headOverBody), PartPose.offset(0, -2.5F, 5));
		head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(12, 8).addBox(-1, -0.5F, 2.5F, 2, 2, 1), PartPose.ZERO);
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(20, 8).addBox(0.5F, -2.5F, 0, 1, 1, 1), PartPose.ZERO);
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(20, 8).mirror().addBox(-1.5F, -2.5F, 0, 1, 1, 1), PartPose.ZERO);
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 14).addBox(-1, -1.5F, -3, 2, 3, 3), PartPose.offset(0, -1.5F, 0));
		tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 20).addBox(-1.5F, -2, -4, 3, 4, 4), PartPose.offset(0, 0, -2.5F));
		body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, 0, -0.5F, 1, 2, 1), PartPose.offset(limbs, -1, 4.4F));
		body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-0.5F, 0, -0.5F, 1, 2, 1), PartPose.offset(-limbs, -1, 4.4F));
		body.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 4).addBox(-0.5F, 0, -1, 1, 2, 2), PartPose.offset(limbs, -1, 1.1F));
		body.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(24, 4).mirror().addBox(-0.5F, 0, -1, 1, 2, 2), PartPose.offset(-limbs, -1, 1.1F));
		return LayerDefinition.create(mesh, 32, 32);
	}
}
