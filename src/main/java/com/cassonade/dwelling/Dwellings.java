package com.cassonade.dwelling;

import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import com.cassonade.Cassonade;
import com.cassonade.tweaks.Taverns;

/**
 * Lone dwellings out in the wild (jigsaw structures of one piece each, their templates edited with /dev edit): a
 * hunter's cabin in the taiga, a shepherd's hut in the plains, a hermit's home in the woods and a fisherman's shack on
 * a river. One structure set holds them all, so a spot of its grid gets whichever fits its biome. Each is lived in,
 * its villager made here rather than kept in the template, as a structure block saves no entity: the villager finds
 * the workstation the template has and takes its trade, but the hermit, a nitwit. Now and then one was left: nobody
 * lives there, its fire and lanterns are out and cobwebs hang under the roof. The fisherman's shack stands on stilts
 * carried down to the river's bed.
 */
public final class Dwellings {
	public static final ResourceKey<Structure> HUNTERS_CABIN = key("hunters_cabin");
	public static final ResourceKey<Structure> SHEPHERDS_HUT = key("shepherds_hut");
	public static final ResourceKey<Structure> HERMITS_HOME = key("hermits_home");
	public static final ResourceKey<Structure> FISHERMANS_SHACK = key("fishermans_shack");
	public static final Set<ResourceKey<Structure>> ALL = Set.of(HUNTERS_CABIN, SHEPHERDS_HUT, HERMITS_HOME, FISHERMANS_SHACK);
	/** One dwelling in this many is left abandoned. */
	private static final int ABANDONED = 4;
	/** How many of the ceiling's corners get a cobweb, in an abandoned one. */
	private static final float COBWEBS = 0.4F;
	/** How far a post goes down to find the river's bed. */
	private static final int MAX_STILT = 24;

	private Dwellings() {
	}

	private static ResourceKey<Structure> key(String name) {
		return ResourceKey.create(Registries.STRUCTURE, Cassonade.id(name));
	}

	/** Which dwelling the structure is, if it is one. */
	public static Optional<ResourceKey<Structure>> of(WorldGenLevel level, Structure structure) {
		return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getResourceKey(structure).filter(ALL::contains);
	}

	/** Whether the dwelling starting in this chunk was left, the same for every chunk it stands in. */
	public static boolean abandoned(WorldGenLevel level, ChunkPos start) {
		return RandomSource.create(level.getSeed() ^ start.pack() * 0x9E3779B97F4A7C15L).nextInt(ABANDONED) == 0;
	}

	/**
	 * Once the part of the dwelling in a chunk stands: its villager by the bed's foot and, at the shepherd's, sheep by
	 * the pen's hay; or, when it was left, its fires out and cobwebs in its corners.
	 */
	public static void furnish(WorldGenLevel level, ResourceKey<Structure> dwelling, ChunkPos start, BoundingBox area) {
		stilts(level, area);
		if (abandoned(level, start)) {
			Taverns.abandon(level, area);
			cobwebs(level, area);
			return;
		}
		for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD) {
				dweller(level, dwelling, pos.relative(state.getValue(BedBlock.FACING).getOpposite()));
			} else if (dwelling == SHEPHERDS_HUT && state.is(Blocks.HAY_BLOCK) && pen(level.getBlockState(pos.below()))) {
				sheep(level, pos);
			}
		}
	}

	private static void dweller(WorldGenLevel level, ResourceKey<Structure> dwelling, BlockPos foot) {
		Villager villager = EntityTypes.VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (villager == null) {
			return;
		}
		villager.snapTo(foot.getX() + 0.5, foot.getY() + 0.6, foot.getZ() + 0.5, 0, 0);
		villager.finalizeSpawn(level, level.getCurrentDifficultyAt(foot), EntitySpawnReason.STRUCTURE, null);
		if (dwelling == HERMITS_HOME) {
			villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.NITWIT));
		}
		villager.setPersistenceRequired();
		level.addFreshEntityWithPassengers(villager);
	}

	/** A sheep beside the hay bale, on the pen's ground. */
	private static void sheep(WorldGenLevel level, BlockPos hay) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos spot = hay.relative(side);
			if (level.getBlockState(spot).isAir() && pen(level.getBlockState(spot.below()))) {
				Mob sheep = EntityTypes.SHEEP.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
				if (sheep == null) {
					return;
				}
				sheep.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, side.toYRot(), 0);
				sheep.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.STRUCTURE, null);
				sheep.setPersistenceRequired();
				level.addFreshEntityWithPassengers(sheep);
				return;
			}
		}
	}

	/** The pen's ground: grass or dirt (the dirt tag leaves the grass block out). */
	private static boolean pen(BlockState ground) {
		return ground.is(Blocks.GRASS_BLOCK) || ground.is(BlockTags.DIRT);
	}

	/**
	 * The posts the template stands on, logs on its bottom layer, carried down to the ground: the fisherman's shack
	 * stands over a river of any depth.
	 */
	private static void stilts(WorldGenLevel level, BoundingBox area) {
		for (int x = area.minX(); x <= area.maxX(); x++) {
			for (int z = area.minZ(); z <= area.maxZ(); z++) {
				BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, area.minY(), z);
				BlockState post = level.getBlockState(pos);
				if (!post.is(BlockTags.LOGS)) {
					continue;
				}
				for (int depth = 0; depth < MAX_STILT && level.getBlockState(pos.move(Direction.DOWN)).canBeReplaced(); depth++) {
					level.setBlock(pos, post, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** Cobwebs in some of the corners under the roof: an empty cell, over an empty one, with the ceiling above it and walls on two sides. */
	private static void cobwebs(WorldGenLevel level, BoundingBox area) {
		for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY() + 1, area.minZ(), area.maxX(), area.maxY() - 1, area.maxZ())) {
			if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN) || !level.getBlockState(pos.below()).isAir()) {
				continue;
			}
			int walls = 0;
			for (Direction side : Direction.Plane.HORIZONTAL) {
				BlockPos wall = pos.relative(side);
				walls += level.getBlockState(wall).isFaceSturdy(level, wall, side.getOpposite()) ? 1 : 0;
			}
			// The cell's own number rather than the level's random, so a chunk generated again gives the same
			if (walls >= 2 && RandomSource.create(pos.asLong()).nextFloat() < COBWEBS) {
				level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}
}
