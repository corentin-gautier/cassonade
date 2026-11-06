package com.cassonade.client.ambience;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

/**
 * Weather on windows. Under the rain, the glass faces it reaches (the air beside them open to the sky, in a biome
 * where it rains) bead with drops, and drops run down them; once it stops, the runs end and the beads dry up one by
 * one over the minute {@link AfterRain} drips for: pixels of the game's size, flat on the glass, in {@link Foam}'s unlit
 * pipeline, so darkened here by the light of the air beside them; as nothing in the game's pixels fades, they come and
 * go and move by whole pixels. In a cold biome, frost in the colors of the game's powder snow creeps over each window
 * (the whole flat stretch of glass, not each block), thickest in its corners, thinner along its edges, ragged: each
 * pixel has its rank, and the frost's reach, shared by every window, grows a pixel every few seconds towards the day's
 * or the deeper night's, so it spreads pixel by pixel rather than all at once, and draws back the same way; glass that
 * just appeared (placed, or come into range) starts clear and catches up at that pace. The raggedness belongs to the
 * pixels of the world, not to the window, so a block placed or broken only changes the frost near it.
 * <p>
 * Only the side of the glass facing the camera is drawn: the far side, seen through it, would double everything. Glass
 * blocks of any color and glass panes, never iron bars. The faces are looked for every second around the player, only
 * in sections whose palette may hold glass, and again at once when a block changes beside glass. Client only: any server.
 */
public final class GlassWeather {
	private static final int RADIUS = 24;
	private static final int HEIGHT = 12;
	private static final int SCAN_INTERVAL = 20;
	private static final float PIXEL = 1 / 16F;
	/** Just off the glass, so the pixels never fight with it for depth. */
	private static final float LIFT = 0.002F;
	/** Where a pane's glass stands in its block, in pixels: 7 to 9. */
	private static final int PANE_NEAR = 7;
	private static final int PANE_FAR = 9;

	/** Beads sitting on a face soaked by the rain, the first ones to go as it dries. */
	private static final int BEADS = 3;
	/** Drops running down it, only while it rains. */
	private static final int RUNS = 2;
	private static final int RUN_LENGTH = 2;
	private static final float RUN_SPEED_MIN = 0.12F;
	private static final float RUN_SPEED_MAX = 0.4F;
	/** Ticks between a run reaching the bottom and the next one starting at the top. */
	private static final int RUN_PAUSE_MIN = 20;
	private static final int RUN_PAUSE_MAX = 120;
	// Water, barely there on the glass: a bluer tint than the glass's own, never white, which reads as snow
	private static final int WATER = ARGB.color(110, 170, 205, 230);
	private static final int RUN_WATER = ARGB.color(140, 185, 215, 238);

	/** How far the frost reaches into a window, in ranks (about pixels from its edges): by day, and at the dead of night. */
	private static final float FROST_DAY = 3;
	private static final float FROST_NIGHT = 7;
	/** Ranks gained or lost a tick: a pixel about every 6 seconds. */
	private static final float FROST_SPEED = 1 / 120F;
	/** Along an edge, away from the corners, the frost reaches less far, by up to so many ranks. */
	private static final float EDGE_THINNING = 4;
	/** Each pixel's rank is ragged by up to so much, clumps of 2 pixels by up to so much more. */
	private static final float RAGGED = 2.5F;
	private static final float CLUMPS = 2;
	// The powder snow overlay's colors: pale cyan, its brighter cyan and near white
	private static final int[] FROST = {ARGB.color(170, 140, 214, 238), ARGB.color(180, 96, 196, 232), ARGB.color(190, 222, 244, 252)};
	/** How dark the sky gets at the dead of night, as {@link Foam} reads it. */
	private static final float DARKEST_SKY = 11;
	/** The light of the darkest corner, so the pixels never vanish into black. */
	private static final float MIN_BRIGHTNESS = 0.15F;

	/**
	 * A face of glass: {@code facingZ} when it faces north or south, {@code positive} towards the axis's positive side,
	 * its plane at {@code plane} along that axis and running along the other from {@code start}, between pixels
	 * {@code from} and {@code to}; {@code y} its bottom. The window it belongs to spans {@code window}: from, to along
	 * the face, bottom and top.
	 */
	private record Face(boolean facingZ, boolean positive, double plane, double start, int y, int from, int to, boolean wet, boolean frost,
			int blockLight, int skyLight, long seed, double[] window) {
		double along0() {
			return start + from * PIXEL;
		}

		double along1() {
			return start + to * PIXEL;
		}

		/** The camera on the side it faces. */
		boolean seenFrom(Vec3 camera) {
			double side = (facingZ ? camera.z : camera.x) - plane;
			return positive ? side > 0 : side < 0;
		}
	}

	private static List<Face> faces = List.of();
	private static int ticks;
	/** How far the frost reaches now, in ranks, the same for every window. */
	private static float frostReach;
	/** The tick each face was first seen, by {@link #cell}, so new glass frosts over from clear. */
	private static Map<String, Long> born = new HashMap<>();
	private static boolean changed;

	private GlassWeather() {
	}

	static void tick(Minecraft minecraft, ClientLevel level) {
		if (!Ambience.overworld(level)) {
			faces = List.of();
			return;
		}
		if (ticks++ % SCAN_INTERVAL == 0 || changed) {
			changed = false;
			faces = scan(level, minecraft.player.blockPosition());
			Map<String, Long> seen = new HashMap<>();
			for (Face face : faces) {
				String key = cell(face, Mth.floor(face.start), face.y);
				seen.put(key, born.getOrDefault(key, level.getGameTime()));
			}
			born = seen;
		}
		float night = Mth.clamp(level.getSkyDarken() / DARKEST_SKY, 0, 1);
		float target = faces.stream().anyMatch(Face::frost) ? Mth.lerp(night, FROST_DAY, FROST_NIGHT) : 0;
		frostReach = frostReach < target ? Math.min(target, frostReach + FROST_SPEED) : Math.max(target, frostReach - FROST_SPEED);
	}

	/** For the tests: the frost at once where it would have crept to, on every window. */
	public static void settleFrost(ClientLevel level) {
		float night = Mth.clamp(level.getSkyDarken() / DARKEST_SKY, 0, 1);
		frostReach = Mth.lerp(night, FROST_DAY, FROST_NIGHT);
		born.replaceAll((key, tick) -> Long.MIN_VALUE / 2);
	}

	/** A block changed in the level: when glass or what is beside it, the faces are looked for again at the next tick. */
	public static void changed(BlockPos pos, BlockState old, BlockState current) {
		if (isGlass(old) || isGlass(current)) {
			changed = true;
			return;
		}
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null) {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				if (isGlass(level.getBlockState(pos.relative(side)))) {
					changed = true;
					return;
				}
			}
		}
	}

	public static float frostReach() {
		return frostReach;
	}

	/** Faces the rain wets now or has wet, and faces frosting over, around the player, for the tests. */
	public static int wetFaces() {
		return (int) faces.stream().filter(Face::wet).count();
	}

	public static int frostFaces() {
		return (int) faces.stream().filter(Face::frost).count();
	}

	public static void rescan() {
		ticks = 0;
	}

	private static boolean isGlassBlock(BlockState state) {
		return state.is(Blocks.GLASS) || state.is(Blocks.TINTED_GLASS) || state.getBlock() instanceof StainedGlassBlock;
	}

	private static boolean isPane(BlockState state) {
		return state.is(Blocks.GLASS_PANE) || state.getBlock() instanceof StainedGlassPaneBlock;
	}

	private static boolean isGlass(BlockState state) {
		return isGlassBlock(state) || isPane(state);
	}

	private static List<Face> scan(ClientLevel level, BlockPos center) {
		List<Face> found = new ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int minY = Math.max(level.getMinY(), center.getY() - HEIGHT);
		int maxY = Math.min(level.getMaxY(), center.getY() + HEIGHT);
		for (int chunkX = SectionPos.blockToSectionCoord(center.getX() - RADIUS); chunkX <= SectionPos.blockToSectionCoord(center.getX() + RADIUS); chunkX++) {
			for (int chunkZ = SectionPos.blockToSectionCoord(center.getZ() - RADIUS); chunkZ <= SectionPos.blockToSectionCoord(center.getZ() + RADIUS); chunkZ++) {
				LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
				if (chunk == null) {
					continue;
				}
				for (int sectionY = SectionPos.blockToSectionCoord(minY); sectionY <= SectionPos.blockToSectionCoord(maxY); sectionY++) {
					LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
					if (section.hasOnlyAir() || !section.maybeHas(GlassWeather::isGlass)) {
						continue;
					}
					for (int y = 0; y < 16; y++) {
						int blockY = SectionPos.sectionToBlockCoord(sectionY) + y;
						if (blockY < minY || blockY > maxY) {
							continue;
						}
						for (int x = 0; x < 16; x++) {
							for (int z = 0; z < 16; z++) {
								BlockState state = section.getBlockState(x, y, z);
								if (isGlass(state)) {
									pos.set(SectionPos.sectionToBlockCoord(chunkX) + x, blockY, SectionPos.sectionToBlockCoord(chunkZ) + z);
									if (pos.distManhattan(center) <= 2 * RADIUS + HEIGHT) {
										faces(level, pos.immutable(), state, found);
									}
								}
							}
						}
					}
				}
			}
		}
		return windows(found);
	}

	/** The faces again, each with the window it belongs to: the faces of one plane, facing the same way, side by side or stacked. */
	private static List<Face> windows(List<Face> faces) {
		Map<String, Face> byCell = new HashMap<>();
		for (Face face : faces) {
			byCell.put(cell(face, Mth.floor(face.start), face.y), face);
		}
		Map<Face, double[]> windows = new IdentityHashMap<>();
		for (Face face : faces) {
			if (windows.containsKey(face)) {
				continue;
			}
			double[] window = {face.along0(), face.along1(), face.y, face.y + 1};
			List<Face> group = new ArrayList<>();
			ArrayDeque<Face> open = new ArrayDeque<>(List.of(face));
			windows.put(face, window);
			while (!open.isEmpty()) {
				Face next = open.poll();
				group.add(next);
				window[0] = Math.min(window[0], next.along0());
				window[1] = Math.max(window[1], next.along1());
				window[2] = Math.min(window[2], next.y);
				window[3] = Math.max(window[3], next.y + 1);
				int along = Mth.floor(next.start);
				for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
					Face beside = byCell.get(cell(next, along + step[0], next.y + step[1]));
					if (beside != null && !windows.containsKey(beside)) {
						windows.put(beside, window);
						open.add(beside);
					}
				}
			}
		}
		List<Face> placed = new ArrayList<>(faces.size());
		for (Face face : faces) {
			placed.add(new Face(face.facingZ, face.positive, face.plane, face.start, face.y, face.from, face.to, face.wet, face.frost, face.blockLight,
					face.skyLight, face.seed, windows.get(face)));
		}
		return placed;
	}

	private static String cell(Face face, int along, int y) {
		return face.facingZ + "/" + face.positive + "/" + face.plane + "/" + along + "/" + y;
	}

	/** A glass block's four sides that show, or a pane's two sides along each of its arms. */
	private static void faces(ClientLevel level, BlockPos pos, BlockState state, List<Face> found) {
		if (isGlassBlock(state)) {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				BlockState beside = level.getBlockState(pos.relative(side));
				if (!isGlassBlock(beside) && !beside.isSolidRender()) {
					add(level, pos, side, side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16 : 0, 0, 16, found);
				}
			}
			return;
		}
		// A pane's glass runs from the middle to each side it reaches, its faces 7 and 9 pixels into the block
		boolean north = state.getValue(CrossCollisionBlock.NORTH);
		boolean south = state.getValue(CrossCollisionBlock.SOUTH);
		boolean west = state.getValue(CrossCollisionBlock.WEST);
		boolean east = state.getValue(CrossCollisionBlock.EAST);
		if (west || east) {
			int from = west ? 0 : PANE_NEAR;
			int to = east ? 16 : PANE_FAR;
			add(level, pos, Direction.NORTH, PANE_NEAR, from, to, found);
			add(level, pos, Direction.SOUTH, PANE_FAR, from, to, found);
		}
		if (north || south) {
			int from = north ? 0 : PANE_NEAR;
			int to = south ? 16 : PANE_FAR;
			add(level, pos, Direction.WEST, PANE_NEAR, from, to, found);
			add(level, pos, Direction.EAST, PANE_FAR, from, to, found);
		}
	}

	/** The face looking to {@code side}, {@code depth} pixels into the block, wet or frosted by the air on that side. */
	private static void add(ClientLevel level, BlockPos pos, Direction side, int depth, int from, int to, List<Face> found) {
		BlockPos air = pos.relative(side);
		BlockState beside = level.getBlockState(air);
		if (beside.isSolidRender()) {
			return;
		}
		boolean wet = AfterRain.wasRainedOn(level, air);
		boolean frost = level.getBiome(air).value().coldEnoughToSnow(air, level.getSeaLevel());
		if (!wet && !frost) {
			return;
		}
		boolean facingZ = side.getAxis() == Direction.Axis.Z;
		boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
		double plane = (facingZ ? pos.getZ() : pos.getX()) + depth * PIXEL + (positive ? LIFT : -LIFT);
		double start = facingZ ? pos.getX() : pos.getZ();
		found.add(new Face(facingZ, positive, plane, start, pos.getY(), from, to, wet, frost, level.getBrightness(LightLayer.BLOCK, air),
				level.getBrightness(LightLayer.SKY, air), pos.asLong() * 31 + side.ordinal(), null));
	}

	static void draw(LevelRenderContext context) {
		if (faces.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}
		float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		boolean raining = level.isRaining();
		float wetness = raining ? level.getRainLevel(partial) : AfterRain.wetness(level);
		float time = level.getGameTime() + partial;
		Vec3 camera = context.levelState().cameraRenderState.pos;
		PoseStack poseStack = context.poseStack();
		List<Face> seen = faces.stream().filter(face -> face.seenFrom(camera)).toList();
		float reach = frostReach;
		long now = level.getGameTime();
		context.submitNodeCollector().submitCustomGeometry(poseStack, Foam.renderType(), (pose, buffer) -> {
			for (Face face : seen) {
				float brightness = brightness(face, level.getSkyDarken());
				// New glass catches up with the rest at the frost's own pace
				float faceReach = Math.min(reach, (now - born.getOrDefault(cell(face, Mth.floor(face.start), face.y), now)) * FROST_SPEED);
				if (face.frost && faceReach > 0) {
					frost(pose, buffer, face, faceReach, brightness, camera);
				}
				if (face.wet && wetness > 0) {
					drops(pose, buffer, face, wetness, raining, time, brightness, camera);
				}
			}
		});
	}

	/** The light of the air beside the face, the sky's dimmed as it darkens, as the game lights a block. */
	private static float brightness(Face face, int skyDarken) {
		int light = Math.max(face.blockLight, Math.max(0, face.skyLight - skyDarken));
		return MIN_BRIGHTNESS + (1 - MIN_BRIGHTNESS) * light / 15F;
	}

	/**
	 * The face's share of its window's frost, row by row, a run of pixels of one color drawn at once. A pixel is
	 * frosted when its rank, from where it lies in the window, is within the frost's reach.
	 */
	private static void frost(PoseStack.Pose pose, VertexConsumer buffer, Face face, float reach, float brightness, Vec3 camera) {
		double[] window = face.window;
		int width = (int) Math.round((window[1] - window[0]) / PIXEL);
		int height = (int) Math.round((window[3] - window[2]) / PIXEL);
		int left = (int) Math.round((face.along0() - window[0]) / PIXEL) - face.from;
		int bottom = (face.y - (int) window[2]) * 16;
		// The raggedness of the world's own pixels: the same whatever the window around them
		long planeSeed = Double.doubleToLongBits(face.plane) * 31 + (face.positive ? 1 : 0);
		int worldLeft = Mth.floor(face.start) * 16;
		for (int row = 0; row < 16; row++) {
			int run = -1;
			int runColor = 0;
			for (int column = face.from; column <= face.to; column++) {
				int color = column < face.to
						? frostColor(left + column, bottom + row, width, height, planeSeed, worldLeft + column, face.y * 16 + row, reach) : 0;
				if (color != runColor) {
					if (run >= 0) {
						rect(pose, buffer, face, run, column, row, row + 1, ARGB.scaleRGB(runColor, brightness), camera);
					}
					run = color != 0 ? column : -1;
					runColor = color;
				}
			}
		}
	}

	/** 0 for a clear pixel of the window, at x, y in it and worldX, worldY in the world's pixels; otherwise its frost's color. */
	private static int frostColor(int x, int y, int width, int height, long seed, int worldX, int worldY, float reach) {
		int fromSide = Math.min(x, width - 1 - x);
		int fromEnd = Math.min(y, height - 1 - y);
		// Thickest in the corners: along an edge, the farther from the other edges, the thinner
		float rank = Math.min(fromSide, fromEnd) + Math.min(EDGE_THINNING, Math.max(fromSide, fromEnd) / 4F);
		long pixel = next(next(seed ^ (worldX * 73856093L) ^ (worldY * 19349663L)));
		long clump = next(next(seed ^ ((worldX >> 1) * 83492791L) ^ ((worldY >> 1) * 2971215073L)));
		rank += (pixel >>> 24 & 0xFF) / 255F * RAGGED + (clump >>> 24 & 0xFF) / 255F * CLUMPS;
		if (rank >= reach) {
			return 0;
		}
		return FROST[(int) Math.floorMod(pixel >>> 40, (long) FROST.length)];
	}

	/** Beads, as many as the face is wet, and while it rains the runs: each from the top, a whole pixel at a time. */
	private static void drops(PoseStack.Pose pose, VertexConsumer buffer, Face face, float wetness, boolean raining, float time, float brightness,
			Vec3 camera) {
		int width = face.to - face.from;
		if (width < 2) {
			return;
		}
		long seed = face.seed;
		int beads = Math.round(BEADS * wetness);
		int water = ARGB.scaleRGB(WATER, brightness);
		for (int i = 0; i < BEADS; i++) {
			seed = next(seed);
			int column = face.from + (int) Math.floorMod(seed, (long) width);
			seed = next(seed);
			int row = 1 + (int) Math.floorMod(seed, 14L);
			if (i < beads) {
				rect(pose, buffer, face, column, column + 1, row, row + 1, water, camera);
			}
		}
		if (!raining) {
			return;
		}
		int runWater = ARGB.scaleRGB(RUN_WATER, brightness);
		for (int i = 0; i < RUNS; i++) {
			seed = next(seed);
			int column = face.from + (int) Math.floorMod(seed, (long) width);
			seed = next(seed);
			float speed = Mth.lerp((seed >>> 8 & 0xFF) / 255F, RUN_SPEED_MIN, RUN_SPEED_MAX);
			int pause = RUN_PAUSE_MIN + (int) Math.floorMod(seed >>> 16, (long) (RUN_PAUSE_MAX - RUN_PAUSE_MIN));
			float fall = 16 / speed;
			float cycle = fall + pause;
			float phase = Math.floorMod(seed >>> 32, 1000L);
			float along = (time + phase) % cycle;
			if (along < fall) {
				int head = 15 - Mth.floor(along * speed);
				rect(pose, buffer, face, column, column + 1, Math.max(0, head), Math.min(16, head + RUN_LENGTH), runWater, camera);
			}
		}
	}

	private static long next(long seed) {
		return seed * 6364136223846793005L + 1442695040888963407L;
	}

	/** A rectangle of whole pixels on the face, columns along it and rows up from its bottom. */
	private static void rect(PoseStack.Pose pose, VertexConsumer buffer, Face face, int column0, int column1, int row0, int row1, int color,
			Vec3 camera) {
		float a0 = (float) (face.start + column0 * PIXEL);
		float a1 = (float) (face.start + column1 * PIXEL);
		float y0 = (float) (face.y + row0 * PIXEL - camera.y);
		float y1 = (float) (face.y + row1 * PIXEL - camera.y);
		if (face.facingZ) {
			float z = (float) (face.plane - camera.z);
			buffer.addVertex(pose, a0 - (float) camera.x, y0, z).setColor(color);
			buffer.addVertex(pose, a1 - (float) camera.x, y0, z).setColor(color);
			buffer.addVertex(pose, a1 - (float) camera.x, y1, z).setColor(color);
			buffer.addVertex(pose, a0 - (float) camera.x, y1, z).setColor(color);
		} else {
			float x = (float) (face.plane - camera.x);
			buffer.addVertex(pose, x, y0, a0 - (float) camera.z).setColor(color);
			buffer.addVertex(pose, x, y0, a1 - (float) camera.z).setColor(color);
			buffer.addVertex(pose, x, y1, a1 - (float) camera.z).setColor(color);
			buffer.addVertex(pose, x, y1, a0 - (float) camera.z).setColor(color);
		}
	}
}
