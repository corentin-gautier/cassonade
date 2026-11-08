package com.cassonade.swallow;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import com.cassonade.Cassonade;

/**
 * Swallows' nests: a block a player places against a wall, made of mud and wheat, and one now and then under the eaves of a
 * village house, as it is built. Swallows come to any of them by day (client/ambience/Swallows), so a base has them only
 * when its player wants them.
 */
public final class SwallowNests {
	private static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Cassonade.id("swallow_nest"));
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Cassonade.id("swallow_nest"));
	public static final Block BLOCK = Registry.register(BuiltInRegistries.BLOCK, BLOCK_KEY,
			new SwallowNestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).sound(SoundType.MUD).strength(0.5F).noCollision().noOcclusion()
					.pushReaction(PushReaction.POPPED).setId(BLOCK_KEY)));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
			new BlockItem(BLOCK, new Item.Properties().useBlockDescriptionPrefix().setId(ITEM_KEY)));
	/** One village house in this many gets a nest, under one of its eaves: about a pair of swallows for 32 by 32 blocks of village. */
	private static final int HOUSE_ODDS = 4;
	/** How high above an eave's outside the air must reach, for it to be open to the sky. */
	private static final int OPEN_ABOVE = 4;

	private SwallowNests() {
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> output.insertAfter(Items.BEE_NEST, ITEM));
	}

	/**
	 * A nest under the house's first eave, one house in HOUSE_ODDS, chosen from its place alone: laid by the chunk holding the
	 * house's middle, from what of the house stands in it, so two chunks never both lay one.
	 */
	public static void inVillageHouse(WorldGenLevel level, StructurePiece house, BoundingBox chunkBB) {
		BoundingBox box = house.getBoundingBox();
		int middleX = (box.minX() + box.maxX()) / 2;
		int middleZ = (box.minZ() + box.maxZ()) / 2;
		if (middleX < chunkBB.minX() || middleX > chunkBB.maxX() || middleZ < chunkBB.minZ() || middleZ > chunkBB.maxZ()
				|| Math.floorMod(mix(BlockPos.asLong(box.minX(), box.minY(), box.minZ())), HOUSE_ODDS) != 0) {
			return;
		}
		for (BlockPos pos : BlockPos.betweenClosed(Math.max(box.minX(), chunkBB.minX()), box.minY() + 2, Math.max(box.minZ(), chunkBB.minZ()),
				Math.min(box.maxX(), chunkBB.maxX()), box.maxY(), Math.min(box.maxZ(), chunkBB.maxZ()))) {
			Direction wall = eave(level, pos);
			if (wall != null) {
				level.setBlock(pos, BLOCK.defaultBlockState().setValue(SwallowNestBlock.FACING, wall.getOpposite()), Block.UPDATE_CLIENTS);
				return;
			}
		}
	}

	/**
	 * The wall the spot is against, if it is an eave: air under a roof, air below it too, a wall on one side, and on the other,
	 * open sky: a roof jutting out over a wall. Never under a tree.
	 */
	private static @Nullable Direction eave(WorldGenLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).isAir() || !built(level.getBlockState(pos.above())) || !level.getBlockState(pos.below()).isAir()) {
			return null;
		}
		for (Direction wall : Direction.Plane.HORIZONTAL) {
			BlockPos beside = pos.relative(wall);
			BlockState state = level.getBlockState(beside);
			BlockPos outside = pos.relative(wall.getOpposite());
			if (built(state) && state.isFaceSturdy(level, beside, wall.getOpposite()) && openAbove(level, outside)) {
				return wall;
			}
		}
		return null;
	}

	private static boolean openAbove(WorldGenLevel level, BlockPos outside) {
		for (int up = 0; up <= OPEN_ABOVE; up++) {
			if (!level.getBlockState(outside.above(up)).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** Something a roof or a wall is made of, not a tree's leaves or trunk. */
	private static boolean built(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
	}

	private static long mix(long value) {
		long z = value + 0x9E3779B97F4A7C15L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return (z ^ (z >>> 31)) & Long.MAX_VALUE;
	}
}
