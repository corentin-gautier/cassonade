package com.cassonade.mushroom;

import java.util.function.Function;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import com.cassonade.Cassonade;

/**
 * The plants of mushroom fields, which grow nowhere else: spore sprouts covering the mycelium, glowcaps lighting
 * it faintly cyan at night, and puffstalks standing two blocks high under a violet puff. Placed as the world
 * generates; like any plant, they can be picked and planted again, on mycelium or podzol.
 */
public final class MyceliumPlants {
	public static final Block SPORE_SPROUTS = block("spore_sprouts", properties -> new MyceliumPlantBlock(12, 7, properties),
			plant().replaceable().sound(SoundType.NETHER_SPROUTS));
	public static final Block GLOWCAP = block("glowcap", properties -> new MyceliumPlantBlock(12, 11, properties),
			plant().sound(SoundType.FUNGUS).lightLevel(state -> 6));
	public static final Block PUFFSTALK = block("puffstalk", PuffstalkBlock::new, plant().sound(SoundType.FUNGUS));
	public static final Item SPORE_SPROUTS_ITEM = item("spore_sprouts", properties -> new BlockItem(SPORE_SPROUTS, properties));
	public static final Item GLOWCAP_ITEM = item("glowcap", properties -> new BlockItem(GLOWCAP, properties));
	public static final Item PUFFSTALK_ITEM = item("puffstalk", properties -> new DoubleHighBlockItem(PUFFSTALK, properties));

	private MyceliumPlants() {
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS)
				.register(output -> output.insertAfter(Items.RED_MUSHROOM, SPORE_SPROUTS_ITEM, GLOWCAP_ITEM, PUFFSTALK_ITEM));
		for (String name : new String[] { "spore_sprouts", "glowcap", "puffstalk" }) {
			BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.MUSHROOM_FIELDS), GenerationStep.Decoration.VEGETAL_DECORATION,
					ResourceKey.create(Registries.PLACED_FEATURE, Cassonade.id(name)));
		}
	}

	private static BlockBehaviour.Properties plant() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollision().instabreak().offsetType(BlockBehaviour.OffsetType.XZ)
				.pushReaction(PushReaction.POPPED);
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Cassonade.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private static Item item(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Cassonade.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().useBlockDescriptionPrefix().setId(key)));
	}
}
