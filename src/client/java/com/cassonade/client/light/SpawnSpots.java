package com.cassonade.client.light;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.cassonade.item.CassonadeItems;

/**
 * Held in either hand, the sculk torch lays a creeper's face, like a faint dark decal, on the blocks around where
 * monsters can spawn: where it is dark enough at any time, and fainter where only the night's darkness lets them
 * (open to a sky brighter than 7). A spot is a block a zombie could spawn on, under two blocks it fits in, with no
 * more block light than the dimension allows. Read on the client from the light it knows, so it works on any
 * server. Drawn as the game's gizmos, flat on the ground and hidden by the blocks in front.
 */
public final class SpawnSpots {
	private static final int RADIUS = 12;
	private static final int HEIGHT = 8;
	/** The spots are looked for twice a second, and the faces follow the view in between. */
	private static final int SCAN_INTERVAL = 10;
	/** Above this sky light, monsters spawn only at night (the game draws the light test from 0 to 7). */
	private static final int NIGHT_ONLY_SKY_LIGHT = 7;
	/** The creeper's face, 8 pixels square: its eyes and mouth, the pixels drawn. */
	private static final String[] FACE = {
			"........",
			"........",
			".XX..XX.",
			".XX..XX.",
			"...XX...",
			"..XXXX..",
			"..XXXX..",
			"..X..X..",
	};
	/** The face's width in blocks, centered on the block, a little above its ground so it is not lost in it. */
	private static final double FACE_BLOCKS = 0.5;
	private static final double LIFT = 0.01;
	private static final int ANY_TIME = 0x66000000;
	private static final int NIGHT_ONLY = 0x33000000;

	/** Where a monster would stand: the block, and the height of what lies there (a carpet, snow), 0 for air. */
	private record Spot(BlockPos pos, double floor, boolean nightOnly) {
		Vec3 ground() {
			return new Vec3(pos.getX() + 0.5, pos.getY() + floor, pos.getZ() + 0.5);
		}
	}

	private static List<Spot> spots = List.of();
	private static int ticks;

	private SpawnSpots() {
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null || !isHeld(player)) {
			spots = List.of();
			return;
		}
		if (ticks++ % SCAN_INTERVAL == 0) {
			spots = scan(minecraft.level, player.blockPosition());
		}
		for (Spot spot : spots) {
			face(spot);
		}
	}

	public static boolean isHeld(LocalPlayer player) {
		return player.getMainHandItem().is(CassonadeItems.SCULK_TORCH) || player.getOffhandItem().is(CassonadeItems.SCULK_TORCH);
	}

	/** How many blocks around monsters could spawn on, now or at night. */
	public static int count() {
		return spots.size();
	}

	/** Whether monsters could spawn standing in this block. */
	public static boolean isMarked(BlockPos pos) {
		return spots.stream().anyMatch(spot -> spot.pos().equals(pos));
	}

	/** The face's dark pixels as flat squares on the spot's ground, a row's neighbors merged into one. */
	private static void face(Spot spot) {
		double pixel = FACE_BLOCKS / FACE.length;
		Vec3 ground = spot.ground();
		double left = ground.x - FACE_BLOCKS / 2;
		double top = ground.z - FACE_BLOCKS / 2;
		double y = ground.y + LIFT;
		GizmoStyle style = GizmoStyle.fill(spot.nightOnly() ? NIGHT_ONLY : ANY_TIME);
		for (int row = 0; row < FACE.length; row++) {
			String line = FACE[row];
			int start = -1;
			for (int column = 0; column <= line.length(); column++) {
				boolean dark = column < line.length() && line.charAt(column) == 'X';
				if (dark && start < 0) {
					start = column;
				} else if (!dark && start >= 0) {
					Gizmos.rect(new Vec3(left + start * pixel, y, top + row * pixel),
							new Vec3(left + column * pixel, y, top + (row + 1) * pixel), Direction.UP, style);
					start = -1;
				}
			}
		}
	}

	private static List<Spot> scan(ClientLevel level, BlockPos center) {
		int blockLightLimit = level.dimensionType().monsterSpawnBlockLightLimit();
		boolean hasSky = level.dimensionType().hasSkyLight();
		List<Spot> found = new ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
					pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (canSpawnAt(level, pos) && level.getBrightness(LightLayer.BLOCK, pos) <= blockLightLimit) {
						boolean nightOnly = hasSky && level.getBrightness(LightLayer.SKY, pos) > NIGHT_ONLY_SKY_LIGHT;
						VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
						found.add(new Spot(pos.immutable(), shape.isEmpty() ? 0 : shape.max(Direction.Axis.Y), nightOnly));
					}
				}
			}
		}
		return found;
	}

	/** A zombie fits standing at pos, on a block it may spawn on (NaturalSpawner's checks for the ground). */
	private static boolean canSpawnAt(ClientLevel level, BlockPos pos) {
		BlockPos below = pos.below();
		if (!level.getBlockState(below).isValidSpawn(level, below, EntityTypes.ZOMBIE)) {
			return false;
		}
		BlockState feet = level.getBlockState(pos);
		BlockState head = level.getBlockState(pos.above());
		return NaturalSpawner.isValidEmptySpawnBlock(level, pos, feet, feet.getFluidState(), EntityTypes.ZOMBIE)
				&& NaturalSpawner.isValidEmptySpawnBlock(level, pos.above(), head, head.getFluidState(), EntityTypes.ZOMBIE);
	}

}
