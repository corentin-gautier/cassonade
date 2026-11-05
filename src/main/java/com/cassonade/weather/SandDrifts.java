package com.cassonade.weather;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import com.cassonade.Cassonade;

/**
 * Sandstorms: when it rains over a desert or the badlands, where the game shows no weather, the wind lays sand on the dunes, a
 * layer at a time, only over sand of its own color, so builds and paths stay clean. It piles up where sand would: in a hollow
 * and at the foot of a dune or a wall on the side away from the wind, up to three layers, a single one on open ground, and
 * none on a crest the wind sweeps. Layers of sand are made and
 * placed like snow layers too, and taken off one at a time with a shovel (SnowShovel). The storm itself is seen on the client
 * (client Storm).
 */
public final class SandDrifts {
	/** The storm stops adding layers here, in the most sheltered places; more can be placed by hand, up to a full block like snow. */
	public static final int STORM_LAYERS = 3;
	/**
	 * One chance in this, each time the game would lay snow there: slower than snow, so a storm leaves sand here and there,
	 * and the sheltered places fill up over a storm or two.
	 */
	private static final int DRIFT_CHANCE = 4;
	private static final int TICKS_PER_DAY = 24000;
	private static final Direction[] SIDES = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
	public static final Block SAND_LAYER = block("sand_layer", MapColor.SAND);
	public static final Block RED_SAND_LAYER = block("red_sand_layer", MapColor.COLOR_ORANGE);
	public static final Item SAND_LAYER_ITEM = item("sand_layer", SAND_LAYER);
	public static final Item RED_SAND_LAYER_ITEM = item("red_sand_layer", RED_SAND_LAYER);

	private SandDrifts() {
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS)
				.register(output -> {
					output.insertAfter(Items.SAND, SAND_LAYER_ITEM);
					output.insertAfter(Items.RED_SAND, RED_SAND_LAYER_ITEM);
				});
	}

	/** A desert or the badlands, where it never rains: when it rains elsewhere, a sandstorm blows there. */
	public static boolean sandstorm(Holder<Biome> biome) {
		return !biome.value().hasPrecipitation() && (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS));
	}

	/**
	 * Where the wind blows to, in radians from east towards south, the same all day and changing from one day to the next:
	 * the storm's grains fly this way (client Storm) and the sand piles up behind what stops it.
	 */
	public static float windBearing(long clockTime) {
		long day = Math.floorDiv(clockTime, TICKS_PER_DAY);
		return (Mth.murmurHash3Mixer((int) day) & 0xFFFF) / 65536.0F * Mth.TWO_PI;
	}

	/** Beside the game's snowfall, at the same random spot of each chunk (ServerLevelMixin). */
	public static void tick(ServerLevel level, BlockPos column) {
		if (!level.isRaining() || level.getRandom().nextInt(DRIFT_CHANCE) != 0) {
			return;
		}
		BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
		if (!sandstorm(level.getBiome(top))) {
			return;
		}
		BlockState state = level.getBlockState(top);
		int most = shelter(level, top);
		if (state.is(SAND_LAYER) || state.is(RED_SAND_LAYER)) {
			int layers = state.getValue(SnowLayerBlock.LAYERS);
			if (layers < most) {
				BlockState higher = state.setValue(SnowLayerBlock.LAYERS, layers + 1);
				Block.pushEntitiesUp(state, higher, level, top);
				level.setBlockAndUpdate(top, higher);
			}
			return;
		}
		if (!state.isAir() || most == 0) {
			return;
		}
		BlockState below = level.getBlockState(top.below());
		if (below.is(Blocks.SAND)) {
			level.setBlockAndUpdate(top, SAND_LAYER.defaultBlockState());
		} else if (below.is(Blocks.RED_SAND)) {
			level.setBlockAndUpdate(top, RED_SAND_LAYER.defaultBlockState());
		}
	}

	/**
	 * How many layers the wind leaves here, from the four sides' ground, in eighths of a block, layers counted: none on a crest
	 * higher than every side, one on open ground, one more in the lee of a side higher than here, upwind, and one more in a
	 * hollow, with two sides or more higher. Sand fills a hollow no higher than its lowest higher side.
	 */
	private static int shelter(ServerLevel level, BlockPos top) {
		float bearing = windBearing(level.getDefaultClockTime());
		float windX = Mth.cos(bearing);
		float windZ = Mth.sin(bearing);
		// The side the wind comes from, along whichever axis it mostly blows
		Direction upwind = Math.abs(windX) > Math.abs(windZ) ? (windX > 0 ? Direction.WEST : Direction.EAST) : (windZ > 0 ? Direction.NORTH : Direction.SOUTH);
		int here = ground(level, top);
		int higher = 0;
		int lower = 0;
		int lowestHigher = Integer.MAX_VALUE;
		boolean lee = false;
		for (Direction side : SIDES) {
			int ground = ground(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, top.relative(side)));
			if (ground > here) {
				higher++;
				lowestHigher = Math.min(lowestHigher, ground);
				lee |= side == upwind;
			} else if (ground < here) {
				lower++;
			}
		}
		if (lower == SIDES.length) {
			return 0;
		}
		int most = 1 + (lee ? 1 : 0) + (higher >= 2 ? 1 : 0);
		int base = here - layers(level.getBlockState(top));
		return higher == 0 ? most : Math.min(most, lowestHigher - base);
	}

	/** The height of the ground at the top of a column, in eighths of a block, with the layers of sand or snow on it. */
	private static int ground(ServerLevel level, BlockPos top) {
		return top.getY() * SnowLayerBlock.MAX_HEIGHT + layers(level.getBlockState(top));
	}

	private static int layers(BlockState state) {
		return state.getBlock() instanceof SnowLayerBlock ? state.getValue(SnowLayerBlock.LAYERS) : 0;
	}

	/** Snow's properties, with sand's sound and color, and no random ticks: sand does not melt. */
	@SuppressWarnings("deprecation")
	private static Block block(String name, MapColor color) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Cassonade.id(name));
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(color).replaceable().forceSolidOff().strength(0.1F)
				.sound(SoundType.SAND).instrument(NoteBlockInstrument.SNARE).pushReaction(PushReaction.POPPED)
				.isViewBlocking((state, level, pos, aabb) -> state.getValue(SnowLayerBlock.LAYERS) >= SnowLayerBlock.MAX_HEIGHT);
		return Registry.register(BuiltInRegistries.BLOCK, key, new SandLayerBlock(properties.setId(key)));
	}

	private static Item item(String name, Block block) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Cassonade.id(name));
		Function<Item.Properties, Item> factory = properties -> new BlockItem(block, properties);
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().useBlockDescriptionPrefix().setId(key)));
	}
}
