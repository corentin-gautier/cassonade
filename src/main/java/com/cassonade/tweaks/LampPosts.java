package com.cassonade.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * The villages' lamp posts: lanterns on posts in the village's wood or stone, saved over vanilla's torch-topped ones
 * (data/minecraft/structure/village/&lt;biome&gt;/&lt;biome&gt;_lamp…, edited in the game with /dev edit) so the
 * streets' decor pools and weights stay vanilla's. The snowy village's already had lanterns and are left as they are.
 * A zombie village's processors only take torches, so its lanterns are taken down like its tavern's.
 * <p>
 * A street that follows the ground lays its path under whatever stands there, a lamp post placed beside an earlier
 * street included: vanilla's torch posts stood in the middle of paths too. Once the village stands, a post whose foot
 * is on a path is taken away.
 */
public final class LampPosts {
	private LampPosts() {
	}

	/** Whether the piece is one of the lamp posts along the streets. */
	public static boolean is(StructurePiece piece) {
		return piece instanceof PoolElementStructurePiece lamp && lamp.getElement().toString().contains("_lamp");
	}

	/**
	 * Takes the part of the lamp post within the area away when its foot stands on a path, so it never blocks a street.
	 * The foot is looked for over the whole post, at most 3 blocks wide, so an arm over the next chunk goes with it.
	 */
	public static void clearStreet(WorldGenLevel level, BoundingBox lamp, BoundingBox area) {
		boolean onPath = false;
		for (int x = lamp.minX(); x <= lamp.maxX() && !onPath; x++) {
			for (int z = lamp.minZ(); z <= lamp.maxZ() && !onPath; z++) {
				BlockPos foot = new BlockPos(x, lamp.minY(), z);
				onPath = !level.getBlockState(foot).isAir() && level.getBlockState(foot.below()).is(Blocks.DIRT_PATH);
			}
		}
		if (onPath) {
			for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}
}
