package com.vanillasugar.item;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.hopper.FilterHopper;

public final class VanillaSugarItems {
	public static final Item BACKPACK = register("backpack", BackpackItem::new,
			new Item.Properties().stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
	public static final Item FILTER_HOPPER = register("filter_hopper", properties -> new BlockItem(FilterHopper.BLOCK, properties),
			new Item.Properties().useBlockDescriptionPrefix().component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));

	private VanillaSugarItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, VanillaSugar.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> output.insertAfter(Items.BUNDLE, BACKPACK));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS)
				.register(output -> output.insertAfter(Items.HOPPER, FILTER_HOPPER));
	}
}
