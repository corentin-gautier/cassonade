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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;
import com.cassonade.swallow.SwallowNestBlock;
import com.cassonade.swallow.SwallowNests;

/**
 * Swallows at their nests (swallow/SwallowNests: a block, now and then under a village house's eaves, or placed by a player): by
 * day, in weather neither cold nor wet, a pair from the nearest one sweeps in loops over the roof, beating then gliding,
 * twittering, and now and then one darts into the nest by way of the air out before it, stays a while, and is off again. On a
 * server without the mod there is no nest, so no swallow. Pictures and sounds on the client, nothing more.
 */
public final class Swallows {
	private static final Identifier TEXTURE = Cassonade.id("textures/entity/swallow.png");
	private static final Identifier TWITTER = Cassonade.id("swallow");
	/** How far around the player nests are looked for, and how often. */
	private static final int REACH = 32;
	private static final int RESCAN = 100;
	/** The swallows come to the nearest nest within this, and go once the player is farther. */
	private static final float NEAR_NEST = 24;
	private static final int PAIR = 2;
	/** How often one more of the pair is looked for a clear loop: often enough to come soon, seldom enough to cost nothing. */
	private static final int ARRIVE_EVERY = 40;
	private static final float SPEED = 0.42F;
	private static final float MIN_RADIUS = 5;
	private static final float MAX_RADIUS = 9;
	/** Above the roof around the nest, and up to this much higher where the loop would meet a block. */
	private static final float ABOVE_ROOF = 2.5F;
	private static final int HIGHER = 8;
	private static final int LOOP_POINTS = 32;
	/** In the nest for a while, never with anyone this close. */
	private static final int MIN_IN_NEST = 60;
	private static final int MAX_IN_NEST = 180;
	private static final float SHY = 4;
	/** Each tick, the chance one circling darts into the nest. */
	private static final int VISIT_ODDS = 400;
	private static final int LEAVING = 60;
	private static final int FADE = 15;
	private static final float SCALE = 0.7F;
	private static final float TWITTER_VOLUME = 0.25F;
	private static final float TWITTER_RANGE = 40;
	private static final float MIN_TEMPERATURE = 0.3F;
	private static final float MAX_TEMPERATURE = 1.5F;
	private static final LayerDefinition MODEL = model();
	private static final RandomSource RANDOM = RandomSource.create();
	private static final List<Nest> NESTS = new ArrayList<>();
	private static final List<Swallow> SWALLOWS = new ArrayList<>();
	private static @Nullable ClientLevel scannedIn;
	private static int rescan;
	private static boolean spawning = true;
	private static int arrive;

	/** A nest: the eave's block it sits in, and the wall it is set against. */
	record Nest(BlockPos pos, Direction wall) {
		/** Out from the wall, the open side. */
		Direction out() {
			return wall.getOpposite();
		}

		Vec3 center() {
			return Vec3.atCenterOf(pos);
		}
	}

	private Swallows() {
	}

	public static int nests() {
		return NESTS.size();
	}

	public static int count() {
		return SWALLOWS.size();
	}

	/** How many are in their nest; for tests. */
	public static long nesting() {
		return SWALLOWS.stream().filter(swallow -> swallow.state == State.NESTING).count();
	}

	/** Whether one is inside a block right now: none should be; for tests. */
	public static boolean anyInBlock() {
		return SWALLOWS.stream().anyMatch(swallow -> !open(swallow.level, swallow.position(0)));
	}

	/** For tests: stops swallows coming on their own and sends the others away at once, or lets them come again. */
	public static void setSpawning(boolean spawning) {
		Swallows.spawning = spawning;
		SWALLOWS.clear();
	}

	/** For tests: looks for the nests again at once, then sends one of the pair into its nest. */
	public static void rescan() {
		rescan = RESCAN;
	}

	public static void visitNow() {
		SWALLOWS.stream().filter(swallow -> swallow.state == State.CIRCLING).filter(swallow -> swallow.visit(true)).findFirst();
	}

	/** The nest nearest to a spot; for tests. */
	public static @Nullable BlockPos nearestNest(Vec3 to) {
		return NESTS.stream().min((a, b) -> Double.compare(a.center().distanceToSqr(to), b.center().distanceToSqr(to))).map(Nest::pos).orElse(null);
	}

	static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null) {
			NESTS.clear();
			SWALLOWS.clear();
			scannedIn = null;
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}
		if (level != scannedIn || ++rescan >= RESCAN) {
			rescan = 0;
			scannedIn = level;
			scan(level, minecraft.player.blockPosition());
		}
		Vec3 player = minecraft.player.position();
		Nest near = NESTS.stream().filter(nest -> nest.center().distanceToSqr(player) < NEAR_NEST * NEAR_NEST)
				.min((a, b) -> Double.compare(a.center().distanceToSqr(player), b.center().distanceToSqr(player))).orElse(null);
		boolean weather = Ambience.overworld(level) && Ambience.daytime(level) && !level.isRaining() && mild(level, minecraft.player.blockPosition());
		for (Swallow swallow : SWALLOWS) {
			if (swallow.state != State.LEAVING && (!weather || !NESTS.contains(swallow.nest) || swallow.nest.center().distanceToSqr(player) > NEAR_NEST * NEAR_NEST * 1.5)) {
				swallow.leave();
			}
			swallow.tick(minecraft);
		}
		SWALLOWS.removeIf(swallow -> swallow.level != level || swallow.state == State.LEAVING && swallow.leaving >= LEAVING);
		if (spawning && weather && near != null && ++arrive >= ARRIVE_EVERY && SWALLOWS.stream().filter(swallow -> swallow.state != State.LEAVING).count() < PAIR) {
			arrive = 0;
			Swallow swallow = arriving(level, near);
			if (swallow != null) {
				SWALLOWS.add(swallow);
			}
		}
	}

	private static boolean mild(ClientLevel level, BlockPos pos) {
		float temperature = level.getBiome(pos).value().getBaseTemperature();
		return temperature >= MIN_TEMPERATURE && temperature < MAX_TEMPERATURE;
	}

	/**
	 * The nests around the player: the swallows' nest blocks, a village's or placed by a player, in the chunks around and the
	 * sections near their height, each section's palette read first so most are passed over without reading a block.
	 */
	private static void scan(ClientLevel level, BlockPos around) {
		NESTS.clear();
		int fromSection = level.getSectionIndex(around.getY() - REACH);
		int toSection = level.getSectionIndex(around.getY() + REACH);
		for (int cx = (around.getX() - REACH) >> 4; cx <= (around.getX() + REACH) >> 4; cx++) {
			for (int cz = (around.getZ() - REACH) >> 4; cz <= (around.getZ() + REACH) >> 4; cz++) {
				if (!level.hasChunk(cx, cz)) {
					continue;
				}
				LevelChunk chunk = level.getChunk(cx, cz);
				LevelChunkSection[] sections = chunk.getSections();
				for (int index = Math.max(fromSection, 0); index <= Math.min(toSection, sections.length - 1); index++) {
					LevelChunkSection section = sections[index];
					if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(SwallowNests.BLOCK))) {
						continue;
					}
					int baseY = level.getMinY() + index * 16;
					for (int y = 0; y < 16; y++) {
						for (int z = 0; z < 16; z++) {
							for (int x = 0; x < 16; x++) {
								BlockState state = section.getBlockState(x, y, z);
								if (state.is(SwallowNests.BLOCK)) {
									NESTS.add(new Nest(new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z),
											state.getValue(SwallowNestBlock.FACING).getOpposite()));
								}
							}
						}
					}
				}
			}
		}
	}

	/** Air where its body is, or its nest. */
	private static boolean open(ClientLevel level, Vec3 at) {
		return passable(level.getBlockState(BlockPos.containing(at))) && passable(level.getBlockState(BlockPos.containing(at.add(0, 0.2, 0))));
	}

	/** Air, or a nest, which a swallow slips into. */
	private static boolean passable(BlockState state) {
		return state.isAir() || state.is(SwallowNests.BLOCK);
	}

	/** Air all along the way, every quarter block. */
	private static boolean clear(ClientLevel level, Vec3 from, Vec3 to) {
		int steps = Math.max(1, Mth.ceil(from.distanceTo(to) * 4));
		for (int i = 1; i <= steps; i++) {
			if (!open(level, from.lerp(to, i / (double) steps))) {
				return false;
			}
		}
		return true;
	}

	/** One of the pair, its loop over the nest's roof, high enough to meet no block. Null when none is clear. */
	private static @Nullable Swallow arriving(ClientLevel level, Nest nest) {
		Vec3 center = nest.center().add(Vec3.atLowerCornerOf(nest.out().getUnitVec3i()).scale(3));
		int roof = 0;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				roof = Math.max(roof, level.getHeight(Heightmap.Types.MOTION_BLOCKING, nest.pos.getX() + dx, nest.pos.getZ() + dz));
			}
		}
		float radiusX = Mth.randomBetween(RANDOM, MIN_RADIUS, MAX_RADIUS);
		float radiusZ = Mth.randomBetween(RANDOM, MIN_RADIUS, MAX_RADIUS);
		for (int higher = 0; higher <= HIGHER; higher++) {
			Swallow swallow = new Swallow(level, nest, new Vec3(center.x, roof + ABOVE_ROOF + higher, center.z), radiusX, radiusZ);
			if (swallow.loopClear()) {
				return swallow;
			}
		}
		return null;
	}

	static void draw(LevelRenderContext context) {
		if (SWALLOWS.isEmpty()) {
			return;
		}
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 camera = context.levelState().cameraRenderState.pos;
		RenderType type = RenderTypes.entityCutout(TEXTURE);
		for (Swallow swallow : SWALLOWS) {
			swallow.draw(context, type, camera, partialTick);
		}
	}

	private enum State {
		CIRCLING, DIVING, NESTING, OUT, LEAVING
	}

	private static final class Swallow {
		/** Beats its wings this long, then glides as long. */
		private static final int BEATING = 10;
		private static final int GLIDING = 14;
		/** Its wings turned back along its sides, folded. */
		private static final float FOLDED = Mth.HALF_PI - 0.15F;

		final ClientLevel level;
		final Nest nest;
		final Vec3 center;
		final float radiusX;
		final float radiusZ;
		final float turning = RANDOM.nextBoolean() ? 1 : -1;
		final float phase = RANDOM.nextFloat() * Mth.TWO_PI;
		final ModelPart root = MODEL.bakeRoot();
		final ModelPart leftWing = root.getChild("left_wing");
		final ModelPart rightWing = root.getChild("right_wing");
		final ModelPart head = root.getChild("head");
		State state = State.CIRCLING;
		float angle = RANDOM.nextFloat() * Mth.TWO_PI;
		int age;
		/** Its way into the nest or out of it, and how far along it. */
		Vec3[] way = new Vec3[0];
		int along;
		int inNest;
		int leaving;
		int twitter = Mth.randomBetweenInclusive(RANDOM, 40, 200);
		Vec3 before;
		Vec3 now;

		Swallow(ClientLevel level, Nest nest, Vec3 center, float radiusX, float radiusZ) {
			this.level = level;
			this.nest = nest;
			this.center = center;
			this.radiusX = radiusX;
			this.radiusZ = radiusZ;
			now = before = loop(angle);
		}

		/** On its loop at this angle, rising and dipping a little. */
		Vec3 loop(float angle) {
			return center.add(Mth.cos(angle) * radiusX, Mth.sin(angle * 2 + phase) * 0.8F, Mth.sin(angle) * radiusZ);
		}

		boolean loopClear() {
			for (int i = 0; i < LOOP_POINTS; i++) {
				float at = i * Mth.TWO_PI / LOOP_POINTS;
				Vec3 a = loop(at);
				Vec3 b = loop(at + Mth.TWO_PI / LOOP_POINTS);
				if (!clear(level, a.add(0, -1, 0), b.add(0, -1, 0)) || !clear(level, a.add(0, 1, 0), b.add(0, 1, 0))) {
					return false;
				}
			}
			return true;
		}

		/** Out before the nest at the loop's height, where it comes down from over the roof, and leaves by. */
		Vec3 approach() {
			Vec3 out = nest.center().add(Vec3.atLowerCornerOf(nest.out().getUnitVec3i()).scale(4));
			return new Vec3(out.x, center.y, out.z);
		}

		/**
		 * Into the nest, by way of the open air out before it, so it never cuts across the roof: false when someone is near it, or
		 * the way is not clear.
		 */
		boolean visit(boolean always) {
			Vec3 entry = nest.center().add(Vec3.atLowerCornerOf(nest.out().getUnitVec3i()).scale(1.5));
			// Clinging to the cup's front, half in it, at the height of its rim
			Vec3 seat = Vec3.atBottomCenterOf(nest.pos).add(Vec3.atLowerCornerOf(nest.wall.getUnitVec3i()).scale(0.15)).add(0, 0.74, 0);
			if (!always && level.players().stream().anyMatch(player -> !player.isSpectator() && player.position().distanceToSqr(nest.center()) < SHY * SHY)) {
				return false;
			}
			Vec3 approach = approach();
			if (!clear(level, now, approach) || !clear(level, approach, entry) || !clear(level, entry, seat)) {
				return false;
			}
			way = path(now, approach, entry, seat);
			along = 0;
			state = State.DIVING;
			return true;
		}

		void leave() {
			state = State.LEAVING;
			leaving = 0;
		}

		void tick(Minecraft minecraft) {
			age++;
			before = now;
			switch (state) {
				case CIRCLING -> {
					angle += turning * SPEED / ((radiusX + radiusZ) / 2);
					now = loop(angle);
					if (--twitter <= 0) {
						twitter = Mth.randomBetweenInclusive(RANDOM, 100, 320);
						AmbientSound.of(TWITTER, TWITTER_VOLUME).from(now).fading(TWITTER_RANGE).pitch(Mth.randomBetween(RANDOM, 0.95F, 1.08F)).play(minecraft);
					}
					if (SWALLOWS.stream().noneMatch(other -> other != this && other.state != State.CIRCLING && other.state != State.LEAVING)
							&& RANDOM.nextInt(VISIT_ODDS) == 0) {
						visit(false);
					}
				}
				case DIVING -> {
					now = way[Math.min(++along, way.length - 1)];
					if (along >= way.length - 1) {
						state = State.NESTING;
						inNest = Mth.randomBetweenInclusive(RANDOM, MIN_IN_NEST, MAX_IN_NEST);
					}
				}
				case NESTING -> {
					if (--inNest <= 0) {
						// Back to its loop where it would be now
						angle += turning * SPEED / ((radiusX + radiusZ) / 2) * (way.length + 1);
						Vec3 entry = nest.center().add(Vec3.atLowerCornerOf(nest.out().getUnitVec3i()).scale(1.5));
						Vec3 approach = approach();
						Vec3 back = loop(angle);
						if (clear(level, now, entry) && clear(level, entry, approach) && clear(level, approach, back)) {
							way = path(now, entry, approach, back);
							along = 0;
							state = State.OUT;
						} else {
							inNest = MIN_IN_NEST;
						}
					}
				}
				case OUT -> {
					now = way[Math.min(++along, way.length - 1)];
					if (along >= way.length - 1) {
						state = State.CIRCLING;
					}
				}
				case LEAVING -> {
					leaving++;
					// Away and up, out of sight
					Vec3 off = now.subtract(center).multiply(1, 0, 1);
					Vec3 heading = off.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : off.normalize();
					Vec3 next = now.add(heading.scale(SPEED)).add(0, 0.12, 0);
					now = open(level, next) ? next : now.add(0, 0.3, 0);
				}
			}
		}

		/** Through the points, a step of its speed each tick. */
		private static Vec3[] path(Vec3... points) {
			List<Vec3> steps = new ArrayList<>();
			steps.add(points[0]);
			for (int i = 1; i < points.length; i++) {
				int count = Math.max(1, Mth.ceil(points[i - 1].distanceTo(points[i]) / SPEED));
				for (int step = 1; step <= count; step++) {
					steps.add(points[i - 1].lerp(points[i], step / (double) count));
				}
			}
			return steps.toArray(Vec3[]::new);
		}

		Vec3 position(float partialTick) {
			return before.lerp(now, partialTick);
		}

		void draw(LevelRenderContext context, RenderType type, Vec3 camera, float partialTick) {
			float size = SCALE;
			if (state == State.LEAVING) {
				size *= Mth.clamp((LEAVING - leaving - partialTick) / FADE, 0, 1);
			} else if (age < FADE) {
				size *= Mth.clamp((age + partialTick) / FADE, 0, 1);
			}
			if (size <= 0) {
				return;
			}
			Vec3 at = position(partialTick);
			Vec3 velocity = now.subtract(before);
			float facing;
			float pitch = 0;
			float roll = 0;
			if (state == State.NESTING) {
				// Sat in the cup, its head out, its wings folded
				facing = (float) Mth.atan2(nest.out().getStepX(), nest.out().getStepZ());
				leftWing.zRot = 0;
				rightWing.zRot = 0;
				leftWing.yRot = FOLDED;
				rightWing.yRot = -FOLDED;
				head.yRot = Mth.sin((age + partialTick) * 0.15F) * 0.5F;
			} else {
				facing = (float) Mth.atan2(velocity.x, velocity.z);
				pitch = (float) -Mth.atan2(velocity.y, velocity.horizontalDistance()) * 0.7F;
				// Banking into its turn on the loop
				roll = state == State.CIRCLING ? -turning * 0.5F : 0;
				float time = age + partialTick;
				int cycle = (int) (time + phase * 10) % (BEATING + GLIDING);
				float spread = cycle < BEATING ? Mth.sin(time * 2.2F) * 0.8F : 0.1F;
				leftWing.zRot = -spread;
				rightWing.zRot = spread;
				leftWing.yRot = 0;
				rightWing.yRot = 0;
				head.yRot = 0;
			}
			BlockPos block = BlockPos.containing(at.add(0, 0.2, 0));
			int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, block), level.getBrightness(LightLayer.SKY, block));
			PoseStack pose = context.poseStack();
			pose.pushPose();
			pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
			pose.rotate(Axis.YP.rotation(facing));
			pose.rotate(Axis.XP.rotation(pitch));
			pose.rotate(Axis.ZP.rotation(roll));
			pose.scale(-size, -size, size);
			context.submitNodeCollector().submitModelPart(root, pose, type, light, OverlayTexture.NO_OVERLAY, null);
			pose.popPose();
		}
	}

	/**
	 * A sixteenth of a block a pixel, its head towards +Z: a slim body, a small head, a long forked tail, and long narrow wings
	 * held out to the sides, beating or gliding.
	 */
	private static LayerDefinition model() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -2, -2.5F, 2, 2, 5), PartPose.ZERO);
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 8).addBox(-1, -2, 0, 2, 2, 2), PartPose.offset(0, -0.5F, 2));
		head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(12, 8).addBox(-0.5F, -1, 2, 1, 1, 1), PartPose.ZERO);
		root.addOrReplaceChild("left_tail", CubeListBuilder.create().texOffs(0, 14).addBox(-0.5F, 0, -5, 1, 1, 5), PartPose.offsetAndRotation(0.5F, -1.5F, -2.5F, 0, -0.25F, 0));
		root.addOrReplaceChild("right_tail", CubeListBuilder.create().texOffs(0, 14).mirror().addBox(-0.5F, 0, -5, 1, 1, 5),
				PartPose.offsetAndRotation(-0.5F, -1.5F, -2.5F, 0, 0.25F, 0));
		root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 22).addBox(0, 0, -1.5F, 6, 1, 3), PartPose.offset(1, -2, 0.5F));
		root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-6, 0, -1.5F, 6, 1, 3), PartPose.offset(-1, -2, 0.5F));
		return LayerDefinition.create(mesh, 32, 32);
	}

}
