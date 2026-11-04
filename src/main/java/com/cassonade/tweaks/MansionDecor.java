package com.cassonade.tweaks;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/** Furnishes a woodland mansion once built: stone bricks instead of cobblestone, lanterns instead of torches, no empty chest, barrels in rooms and corridors, and in rooms candles and pots on tables and lanterns from the ceilings. Decided by position alone, as a room spanning two chunks is furnished once per chunk. */
public final class MansionDecor {
	private static final int LANTERN_SPACING = 5;
	private static final int LANTERN_CLEARANCE = 3;
	/** Rooms stand at most this high: lower than that with nothing beneath is the air outside. */
	private static final int ROOM_HEIGHT = 9;
	private static final Block[] POTTED = {Blocks.POTTED_RED_TULIP, Blocks.POTTED_ALLIUM, Blocks.POTTED_AZURE_BLUET, Blocks.POTTED_OXEYE_DAISY, Blocks.POTTED_FERN};

	private static final int BARREL_RARITY = 20;
	private static final ResourceKey<LootTable> BARREL = lootTable("mansion_barrel");
	private static final ResourceKey<LootTable> TEMPLATE_BARREL = lootTable("mansion_barrel_template");

	private static ResourceKey<LootTable> lootTable(String name) {
		return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("cassonade", "chests/" + name));
	}

	private MansionDecor() {
	}

	/** The rooms holding a backpack template this mansion: none to two, chosen from what all its chunks agree on. */
	public static Set<Integer> templateRooms(int rooms, BoundingBox mansion) {
		RandomSource random = RandomSource.create(mansion.minX() * 341873128712L + mansion.minZ() * 132897987541L);
		Set<Integer> chosen = new HashSet<>();
		int count = random.nextInt(3);
		for (int i = 0; i < count && rooms > 0; i++) {
			chosen.add(random.nextInt(rooms));
		}
		return chosen;
	}

	/** The first free spot along a wall of the room. Called only for the chunk at the room's center, so a room spanning two chunks holds a single one. */
	public static void placeTemplateBarrel(WorldGenLevel level, BoundingBox area) {
		for (BlockPos mutable : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
			BlockPos pos = mutable.immutable();
			Direction wall = wallBeside(level, pos);
			if (wall != null) {
				placeBarrel(level, pos, wall, TEMPLATE_BARREL);
				return;
			}
		}
	}

	/**
	 * Barrels stand on the floor against a wall, leaving the way ahead and any door clear, and take the place of a carpet rather than sit on it.
	 * Not near a chunk's edge: the next chunk may still be bare terrain, its stone passing for a wall in the middle of a corridor.
	 */
	private static @Nullable Direction wallBeside(WorldGenLevel level, BlockPos pos) {
		int x = pos.getX() & 15;
		int z = pos.getZ() & 15;
		if (x < 2 || x > 13 || z < 2 || z > 13 || !isOpen(level.getBlockState(pos)) || !level.getBlockState(pos.above()).isAir() || !level.getBlockState(pos.below()).isSolidRender()) {
			return null;
		}
		Direction wall = null;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos next = pos.relative(side);
			if (level.getBlockState(next).is(BlockTags.DOORS) || level.getBlockState(pos.relative(side, 2)).is(BlockTags.DOORS)) {
				return null;
			}
			if (wall == null && level.getBlockState(next).isSolidRender() && level.getBlockState(next.above()).isSolidRender() && level.getBlockState(next.above(2)).isSolidRender()
					&& isOpen(level.getBlockState(pos.relative(side.getOpposite()))) && isOpen(level.getBlockState(pos.relative(side.getOpposite(), 2)))) {
				wall = side;
			}
		}
		return wall;
	}

	private static boolean isOpen(BlockState state) {
		return state.isAir() || state.is(BlockTags.WOOL_CARPETS);
	}

	private static void placeBarrel(WorldGenLevel level, BlockPos pos, Direction wall, ResourceKey<LootTable> loot) {
		level.setBlock(pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, wall.getOpposite()), 2);
		if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity barrel) {
			barrel.setLootTable(loot, RandomSource.create(pos.asLong()).nextLong());
		}
	}

	public static void furnish(WorldGenLevel level, BoundingBox area, boolean room, boolean corridor) {
		for (BlockPos mutable : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
			BlockPos pos = mutable.immutable();
			BlockState state = level.getBlockState(pos);
			BlockState stone = StoneBricks.of(state, pos);
			if (stone != null) {
				level.setBlock(pos, stone, 2);
			} else if (state.is(Blocks.TORCH)) {
				level.setBlock(pos, Blocks.LANTERN.defaultBlockState(), 2);
			} else if (state.is(Blocks.WALL_TORCH)) {
				replaceWallTorch(level, pos, state.getValue(WallTorchBlock.FACING));
			} else if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest) {
				if (chest.getLootTable() == null && chest.isEmpty()) {
					chest.setLootTable(BuiltInLootTables.WOODLAND_MANSION, RandomSource.create(pos.asLong()).nextLong());
				}
			} else if ((room || corridor) && isOpen(state) && RandomSource.create(pos.asLong() ^ BARREL_RARITY).nextInt(BARREL_RARITY) == 0 && wallBeside(level, pos) != null) {
				placeBarrel(level, pos, wallBeside(level, pos), BARREL);
			} else if (room && state.isAir()) {
				furnishAir(level, pos);
			}
		}
	}

	/** Swaps the cobblestone the mansion's builder poured under it, down to the ground, for the same stone bricks as its walls. */
	public static void foundation(WorldGenLevel level, BoundingBox chunkBB, BoundingBox mansion) {
		for (int x = Math.max(chunkBB.minX(), mansion.minX()); x <= Math.min(chunkBB.maxX(), mansion.maxX()); x++) {
			for (int z = Math.max(chunkBB.minZ(), mansion.minZ()); z <= Math.min(chunkBB.maxZ(), mansion.maxZ()); z++) {
				for (BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, mansion.minY() - 1, z); pos.getY() > level.getMinY(); pos.move(Direction.DOWN)) {
					BlockState stone = StoneBricks.of(level.getBlockState(pos), pos);
					if (stone == null) {
						break;
					}
					level.setBlock(pos, stone, 2);
				}
			}
		}
	}

	/** No lantern hangs on a wall: it hangs from a fence post set against the wall, a block higher. */
	private static void replaceWallTorch(WorldGenLevel level, BlockPos pos, Direction facing) {
		BlockPos above = pos.above();
		if (level.getBlockState(above).isAir()) {
			level.setBlock(above, Blocks.DARK_OAK_FENCE.defaultBlockState().setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(facing.getOpposite()), true), 2);
		}
		level.setBlock(pos, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
	}

	private static void furnishAir(WorldGenLevel level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		RandomSource random = RandomSource.create(pos.asLong());
		if (below.is(BlockTags.WOODEN_FENCES) || below.is(BlockTags.WOODEN_SLABS) && level.getBlockState(pos.below(2)).is(BlockTags.WOODEN_FENCES)) {
			int choice = random.nextInt(12);
			if (choice == 0) {
				level.setBlock(pos, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + random.nextInt(3)).setValue(CandleBlock.LIT, true), 2);
			} else if (choice == 1) {
				level.setBlock(pos, POTTED[random.nextInt(POTTED.length)].defaultBlockState(), 2);
			}
		} else if (pos.getX() % LANTERN_SPACING == 0 && pos.getZ() % LANTERN_SPACING == 0 && hangsInRoom(level, pos)) {
			level.setBlock(pos, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
		}
	}

	/** Under a ceiling, with room to walk beneath, above a floor. */
	private static boolean hangsInRoom(WorldGenLevel level, BlockPos pos) {
		if (!level.getBlockState(pos.above()).is(BlockTags.PLANKS)) {
			return false;
		}
		for (int down = 1; down <= LANTERN_CLEARANCE; down++) {
			if (!level.getBlockState(pos.below(down)).isAir()) {
				return false;
			}
		}
		for (int down = LANTERN_CLEARANCE + 1; down <= ROOM_HEIGHT; down++) {
			if (!level.getBlockState(pos.below(down)).isAir()) {
				return true;
			}
		}
		return false;
	}
}
