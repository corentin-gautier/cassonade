package com.cassonade.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.joml.Vector3fc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.extract.TransientBlock;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Doors, trapdoors and fence gates swing open and shut instead of snapping. Only the picture, on the client: the block
 * changes at once as in vanilla, and for a few ticks its model is left out of the section's mesh (SectionCompilerMixin)
 * and drawn turning about its hinge here. The hinge is found from the closed and open models themselves, so any door
 * shaped like the game's swings right. Once the swing ends, the game's transient block keeps it drawn until the section
 * is built again with it.
 */
public final class SwingingDoors {
	/** A whole swing, as quick as the click's sound. */
	private static final int TICKS = 5;
	/** Beyond this, a door changing is only seen far off: it snaps, as in vanilla. */
	private static final double RANGE = 64;
	private static final Direction[] DIRECTIONS = Direction.values();
	private static final float EPSILON = 1e-3F;

	/** Blocks left out of their section's mesh while they swing, read by the threads building sections. */
	private static final Set<Long> HIDDEN = ConcurrentHashMap.newKeySet();
	private static final Map<Long, Swing> SWINGS = new HashMap<>();
	private static @Nullable ClientLevel swingsLevel;

	private SwingingDoors() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(SwingingDoors::tick);
		LevelRenderEvents.COLLECT_SUBMITS.register(SwingingDoors::draw);
	}

	/** Whether the section being built leaves this block to the swing. */
	public static boolean hidden(BlockPos pos) {
		return !HIDDEN.isEmpty() && HIDDEN.contains(pos.asLong());
	}

	/** Whether a block is swinging, for the tests. */
	public static boolean swinging(BlockPos pos) {
		return SWINGS.containsKey(pos.asLong());
	}

	/** How far open a swinging block stands, 0 to 1, or -1 when it is not swinging, for the tests. */
	public static float openness(BlockPos pos) {
		Swing swing = SWINGS.get(pos.asLong());
		return swing == null ? -1 : swing.openness;
	}

	/** A block changed on the client, before its section is marked to be built again. */
	public static void changed(ClientLevel level, BlockPos pos, BlockState old, BlockState now) {
		long key = pos.asLong();
		Swing swing = SWINGS.get(key);
		if (swing != null && now.getBlock() == swing.state.getBlock()
				&& now.getValue(BlockStateProperties.OPEN) == swing.state.getValue(BlockStateProperties.OPEN)) {
			// A detail changing on the way, such as a door getting powered: it goes on swinging, to stand as it now is
			swing.state = now;
			return;
		}
		if (!swings(old, now)) {
			if (swing != null) {
				stop(key);
			}
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (level != swingsLevel || minecraft.gameRenderer.mainCamera().position().distanceToSqr(Vec3.atCenterOf(pos)) > RANGE * RANGE) {
			return;
		}
		boolean open = now.getValue(BlockStateProperties.OPEN);
		// Always the closed model, turned: closing swings it back from where it stands open
		BlockState closed = open ? now.setValue(BlockStateProperties.OPEN, false) : now;
		Geometry geometry = geometry(closed, pos);
		if (geometry == null) {
			if (swing != null) {
				stop(key);
			}
			return;
		}
		if (swing == null) {
			float from = old.getValue(BlockStateProperties.OPEN) ? 1 : 0;
			swing = new Swing(pos.immutable(), from);
			SWINGS.put(key, swing);
		}
		swing.target = open ? 1 : 0;
		swing.state = now;
		swing.closed = closed;
		swing.geometry = geometry;
		HIDDEN.add(key);
	}

	private static boolean swings(BlockState old, BlockState now) {
		return old.getBlock() == now.getBlock()
				&& (now.getBlock() instanceof DoorBlock || now.getBlock() instanceof TrapDoorBlock || now.getBlock() instanceof FenceGateBlock)
				&& old.getValue(BlockStateProperties.OPEN) != now.getValue(BlockStateProperties.OPEN);
	}

	private static void tick(Minecraft minecraft) {
		if (minecraft.level != swingsLevel) {
			SWINGS.clear();
			HIDDEN.clear();
			swingsLevel = minecraft.level;
		}
		if (SWINGS.isEmpty()) {
			return;
		}
		List<Long> done = new ArrayList<>();
		for (Map.Entry<Long, Swing> entry : SWINGS.entrySet()) {
			Swing swing = entry.getValue();
			swing.openO = swing.openness;
			swing.openness = Mth.approach(swing.openness, swing.target, 1F / TICKS);
			if (swing.openO == swing.target) {
				done.add(entry.getKey());
			}
		}
		for (long key : done) {
			Swing swing = SWINGS.get(key);
			// Drawn as it stands until the section is built again with it, then that section's mesh takes over
			minecraft.levelExtractor.queueTransientBlock(new TransientBlock(swing.pos, swing.state, TransientBlock.DEFAULT_TIME_TO_LIVE, Util.getNanos()));
			stop(key);
			minecraft.levelExtractor.blockChanged(swing.pos, 8);
		}
	}

	private static void stop(long key) {
		SWINGS.remove(key);
		HIDDEN.remove(key);
	}

	private static void draw(LevelRenderContext context) {
		if (SWINGS.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}
		float partialTicks = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 camera = context.levelState().cameraRenderState.pos;
		PoseStack poseStack = context.poseStack();
		for (Swing swing : SWINGS.values()) {
			float openness = Mth.lerp(partialTicks, swing.openO, swing.openness);
			// Eased at both ends, as a door pushed and slowing against its stop
			float eased = openness * openness * (3 - 2 * openness);
			MovingBlockRenderState block = new Lit(level);
			block.blockPos = swing.pos;
			block.randomSeedPos = swing.pos;
			block.blockState = swing.closed;
			block.biome = level.getBiome(swing.pos);
			block.cardinalLighting = level.cardinalLighting();
			block.lightEngine = level.getLightEngine();
			poseStack.pushPose();
			poseStack.translate(swing.pos.getX() - camera.x, swing.pos.getY() - camera.y, swing.pos.getZ() - camera.z);
			Geometry geometry = swing.geometry;
			if (!geometry.fixed.isEmpty()) {
				submit(context, poseStack, block, geometry.fixed::contains);
			}
			for (Leaf leaf : geometry.leaves) {
				poseStack.pushPose();
				poseStack.translate(leaf.pivot.x, leaf.pivot.y, leaf.pivot.z);
				poseStack.rotateDegrees(leaf.axis, leaf.degrees * eased);
				poseStack.translate(-leaf.pivot.x, -leaf.pivot.y, -leaf.pivot.z);
				submit(context, poseStack, block, leaf.quads::contains);
				poseStack.popPose();
			}
			poseStack.popPose();
		}
	}

	/** Some of the block's quads, drawn as the game draws a moving block (a piston's), its light and shading with them. */
	private static void submit(LevelRenderContext context, PoseStack poseStack, MovingBlockRenderState block, Predicate<BakedQuad> keep) {
		Minecraft minecraft = Minecraft.getInstance();
		BlockStateModel model = new Some(minecraft.getModelManager().getBlockStateModelSet().get(block.blockState), keep);
		boolean ambientOcclusion = minecraft.options.ambientOcclusion().get();
		context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, buffer) -> {
			ModelBlockRenderer renderer = new ModelBlockRenderer(ambientOcclusion, false, minecraft.getBlockColors());
			renderer.tesselateBlock((x, y, z, quad, instance) -> buffer.putBakedQuad(pose, quad, instance), 0, 0, 0, block, block.blockPos,
					block.blockState, model, block.blockState.getSeed(block.randomSeedPos));
		});
	}

	/**
	 * What stays and what turns, from the closed and open models: quads found in both stay (a gate's posts), the others
	 * turn, a gate's two leaves each on its own. Null when no quarter turn takes the closed model to the open one.
	 */
	private static @Nullable Geometry geometry(BlockState closed, BlockPos pos) {
		BlockState open = closed.setValue(BlockStateProperties.OPEN, true);
		List<BakedQuad> closedQuads = quads(closed, pos);
		List<BakedQuad> openQuads = quads(open, pos);
		Set<String> closedKeys = new HashSet<>();
		Set<String> openKeys = new HashSet<>();
		closedQuads.forEach(quad -> closedKeys.add(key(quad)));
		openQuads.forEach(quad -> openKeys.add(key(quad)));
		Set<BakedQuad> fixed = Collections.newSetFromMap(new IdentityHashMap<>());
		List<BakedQuad> turningClosed = new ArrayList<>();
		List<BakedQuad> turningOpen = new ArrayList<>();
		for (BakedQuad quad : closedQuads) {
			if (openKeys.contains(key(quad))) {
				fixed.add(quad);
			} else {
				turningClosed.add(quad);
			}
		}
		for (BakedQuad quad : openQuads) {
			if (!closedKeys.contains(key(quad))) {
				turningOpen.add(quad);
			}
		}
		List<Leaf> leaves = new ArrayList<>();
		if (closed.getBlock() instanceof FenceGateBlock) {
			// Each leaf on its own post: split across the gate's width, the same way open and closed
			Direction.Axis width = closed.getValue(FenceGateBlock.FACING).getClockWise().getAxis();
			for (boolean first : new boolean[] {true, false}) {
				Leaf leaf = leaf(side(turningClosed, width, first), side(turningOpen, width, first));
				if (leaf == null) {
					return null;
				}
				leaves.add(leaf);
			}
		} else {
			Leaf leaf = leaf(turningClosed, turningOpen);
			if (leaf == null) {
				return null;
			}
			leaves.add(leaf);
		}
		return new Geometry(fixed, leaves);
	}

	private static List<BakedQuad> side(List<BakedQuad> quads, Direction.Axis width, boolean first) {
		List<BakedQuad> side = new ArrayList<>();
		for (BakedQuad quad : quads) {
			float center = 0;
			for (int vertex = 0; vertex < 4; vertex++) {
				center += (float) width.choose(quad.position(vertex).x(), quad.position(vertex).y(), quad.position(vertex).z()) / 4;
			}
			if (center < 0.5F == first) {
				side.add(quad);
			}
		}
		return side;
	}

	/**
	 * The quarter turn taking these closed quads onto the open ones: about the one axis along which both stand the same,
	 * and of the two pivots that would do it, the one nearest to them, where a hinge sits.
	 */
	private static @Nullable Leaf leaf(List<BakedQuad> closedQuads, List<BakedQuad> openQuads) {
		if (closedQuads.isEmpty() || openQuads.isEmpty()) {
			return null;
		}
		AABB from = box(closedQuads);
		AABB to = box(openQuads);
		for (Direction.Axis axis : Direction.Axis.values()) {
			if (!same(from.min(axis), to.min(axis)) || !same(from.max(axis), to.max(axis))) {
				continue;
			}
			// The plane turned in, in the axis's right-handed order, so that a positive angle is the pose's own rotation
			Direction.Axis u = Direction.Axis.values()[(axis.ordinal() + 1) % 3];
			Direction.Axis v = Direction.Axis.values()[(axis.ordinal() + 2) % 3];
			double au = (from.min(u) + from.max(u)) / 2;
			double av = (from.min(v) + from.max(v)) / 2;
			double bu = (to.min(u) + to.max(u)) / 2;
			double bv = (to.min(v) + to.max(v)) / 2;
			Leaf best = null;
			double bestDistance = Double.MAX_VALUE;
			for (int sign : new int[] {1, -1}) {
				// Pivot c where b - c = R(a - c), R a quarter turn: (u, v) to (-sign v, sign u)
				double d0 = bu + sign * av;
				double d1 = bv - sign * au;
				double cu = (d0 - sign * d1) / 2;
				double cv = (sign * d0 + d1) / 2;
				if (!turnsOnto(from, to, u, v, cu, cv, sign)) {
					continue;
				}
				double distance = Math.min(distance(from, u, v, cu, cv), distance(to, u, v, cu, cv));
				if (distance < bestDistance) {
					bestDistance = distance;
					Vec3 pivot = Vec3.ZERO.with(u, cu).with(v, cv).with(axis, (from.min(axis) + from.max(axis)) / 2);
					Axis rotation = switch (axis) {
						case X -> Axis.XP;
						case Y -> Axis.YP;
						case Z -> Axis.ZP;
					};
					Set<BakedQuad> quads = Collections.newSetFromMap(new IdentityHashMap<>());
					quads.addAll(closedQuads);
					best = new Leaf(quads, pivot, rotation, 90F * sign);
				}
			}
			if (best != null) {
				return best;
			}
		}
		return null;
	}

	private static boolean turnsOnto(AABB from, AABB to, Direction.Axis u, Direction.Axis v, double cu, double cv, int sign) {
		double minU = Double.MAX_VALUE;
		double maxU = -Double.MAX_VALUE;
		double minV = Double.MAX_VALUE;
		double maxV = -Double.MAX_VALUE;
		for (double pu : new double[] {from.min(u), from.max(u)}) {
			for (double pv : new double[] {from.min(v), from.max(v)}) {
				double ru = cu - sign * (pv - cv);
				double rv = cv + sign * (pu - cu);
				minU = Math.min(minU, ru);
				maxU = Math.max(maxU, ru);
				minV = Math.min(minV, rv);
				maxV = Math.max(maxV, rv);
			}
		}
		return same(minU, to.min(u)) && same(maxU, to.max(u)) && same(minV, to.min(v)) && same(maxV, to.max(v));
	}

	private static double distance(AABB box, Direction.Axis u, Direction.Axis v, double cu, double cv) {
		double du = Math.max(0, Math.max(box.min(u) - cu, cu - box.max(u)));
		double dv = Math.max(0, Math.max(box.min(v) - cv, cv - box.max(v)));
		return Math.sqrt(du * du + dv * dv);
	}

	private static boolean same(double a, double b) {
		return Math.abs(a - b) < EPSILON;
	}

	private static AABB box(List<BakedQuad> quads) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE;
		for (BakedQuad quad : quads) {
			for (int vertex = 0; vertex < 4; vertex++) {
				Vector3fc position = quad.position(vertex);
				minX = Math.min(minX, position.x());
				minY = Math.min(minY, position.y());
				minZ = Math.min(minZ, position.z());
				maxX = Math.max(maxX, position.x());
				maxY = Math.max(maxY, position.y());
				maxZ = Math.max(maxZ, position.z());
			}
		}
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static List<BakedQuad> quads(BlockState state, BlockPos pos) {
		BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(state.getSeed(pos)), parts);
		List<BakedQuad> quads = new ArrayList<>();
		for (BlockStateModelPart part : parts) {
			for (Direction direction : DIRECTIONS) {
				quads.addAll(part.getQuads(direction));
			}
			quads.addAll(part.getQuads(null));
		}
		return quads;
	}

	/** Where a quad stands, rounded: a gate's posts are the same in its closed and open models. */
	private static String key(BakedQuad quad) {
		StringBuilder key = new StringBuilder();
		for (int vertex = 0; vertex < 4; vertex++) {
			Vector3fc position = quad.position(vertex);
			key.append(Math.round(position.x() * 1000)).append(',').append(Math.round(position.y() * 1000)).append(',')
					.append(Math.round(position.z() * 1000)).append(';');
		}
		return key.toString();
	}

	private static final class Swing {
		final BlockPos pos;
		float openness;
		float openO;
		float target;
		BlockState state;
		BlockState closed;
		Geometry geometry;

		Swing(BlockPos pos, float openness) {
			this.pos = pos;
			this.openness = openness;
			this.openO = openness;
		}
	}

	private record Geometry(Set<BakedQuad> fixed, List<Leaf> leaves) {
	}

	private record Leaf(Set<BakedQuad> quads, Vec3 pivot, Axis axis, float degrees) {
	}

	/**
	 * Among its real neighbors, so that its shading next to the ground or a wall is the one its section gives it, and it
	 * does not light up as it starts to swing. A solid neighbor's light is its own, though: the game leaves out the faces
	 * against one, which the swing draws, and a trapdoor's underside would take the floor's darkness.
	 */
	private static final class Lit extends MovingBlockRenderState {
		private final ClientLevel level;

		Lit(ClientLevel level) {
			this.level = level;
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			return pos.equals(blockPos) ? blockState : level.getBlockState(pos);
		}

		@Override
		public int getBrightness(LightLayer layer, BlockPos pos) {
			return super.getBrightness(layer, pos.equals(blockPos) || level.getBlockState(pos).isSolidRender() ? blockPos : pos);
		}
	}

	/** A model keeping only some of its quads. */
	private record Some(BlockStateModel model, Predicate<BakedQuad> keep) implements BlockStateModel {
		@Override
		public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
			List<BlockStateModelPart> parts = new ArrayList<>();
			model.collectParts(random, parts);
			for (BlockStateModelPart part : parts) {
				output.add(new SomePart(part, keep));
			}
		}

		@Override
		public Material.Baked particleMaterial() {
			return model.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return model.materialFlags();
		}
	}

	private record SomePart(BlockStateModelPart part, Predicate<BakedQuad> keep) implements BlockStateModelPart {
		@Override
		public List<BakedQuad> getQuads(@Nullable Direction direction) {
			return part.getQuads(direction).stream().filter(keep).toList();
		}

		@Override
		public boolean useAmbientOcclusion() {
			return part.useAmbientOcclusion();
		}

		@Override
		public Material.Baked particleMaterial() {
			return part.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return part.materialFlags();
		}
	}
}
