package com.cassonade.woodcutter;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import com.cassonade.Cassonade;

/**
 * The woodcutter: the stonecutter's twin for planks, made of an iron ingot and three planks. It opens the
 * stonecutter's window and works the same way, on stonecutting recipes (data/cassonade/recipe/woodcutter,
 * written by tools/woodcutter_recipes.py) whose input is a plank. StonecutterMenuMixin keeps the two apart:
 * the stonecutter never offers what the woodcutter does, and the other way round.
 */
public final class Woodcutter {
	private static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, Cassonade.id("woodcutter"));
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Cassonade.id("woodcutter"));
	public static final Block BLOCK = Registry.register(BuiltInRegistries.BLOCK, BLOCK_KEY,
			new WoodcutterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
					.sound(SoundType.WOOD).strength(2.5F).ignitedByLava().setId(BLOCK_KEY)));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
			new BlockItem(BLOCK, new Item.Properties().useBlockDescriptionPrefix().setId(ITEM_KEY)));
	public static final MenuType<WoodcutterMenu> MENU = Registry.register(BuiltInRegistries.MENU, Cassonade.id("woodcutter"),
			new MenuType<>(WoodcutterMenu::new, FeatureFlags.VANILLA_SET));

	private Woodcutter() {
	}

	/** Whether this is a plank: what the woodcutter works on, and the stonecutter does not. */
	public static boolean isWoodInput(ItemStack stack) {
		return stack.is(ItemTags.PLANKS);
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(output -> output.insertAfter(Items.STONECUTTER, ITEM));
	}
}
