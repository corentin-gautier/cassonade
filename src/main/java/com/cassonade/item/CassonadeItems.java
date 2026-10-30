package com.cassonade.item;

import java.util.List;
import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.item.component.ItemContainerContents;

import com.cassonade.Cassonade;

public final class CassonadeItems {
	public static final Item BACKPACK = register("backpack", BackpackItem::new,
			new Item.Properties().stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
	/** The backpack with BACKPACK_UPGRADE_TEMPLATE and a hopper at a smithing table: it serves the player on its own (OrganizedBackpack). */
	public static final Item ORGANIZED_BACKPACK = register("organized_backpack", BackpackItem::new,
			new Item.Properties().stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
	/** Found in the late game's chests only (OrganizedBackpack). */
	public static final Item BACKPACK_UPGRADE_TEMPLATE = register("backpack_upgrade_smithing_template", properties -> new SmithingTemplateItem(
			Component.translatable("item.cassonade.backpack_upgrade_smithing_template.applies_to").withStyle(ChatFormatting.BLUE),
			Component.translatable("item.cassonade.backpack_upgrade_smithing_template.ingredients").withStyle(ChatFormatting.BLUE),
			Component.translatable("item.cassonade.backpack_upgrade_smithing_template.base_slot_description"),
			Component.translatable("item.cassonade.backpack_upgrade_smithing_template.additions_slot_description"),
			List.of(Cassonade.id("container/slot/backpack")), List.of(), properties), new Item.Properties());
	/** Worn in the map gear slot to unlock the world map. */
	public static final Item WORLD_MAP = register("world_map", Item::new, new Item.Properties().stacksTo(1));
	/** A clock with a lightning rod, worn in the clock's slot: the clock's panel also tells the next weather. */
	public static final Item BAROMETER = register("barometer", Item::new, new Item.Properties().stacksTo(1));
	/** A compass with a sculk sensor, worn in the compass's slot: the minimap, and the mobs' health and who hunts the player. */
	public static final Item SCULK_COMPASS = register("sculk_compass", Item::new, new Item.Properties().stacksTo(1));
	/** A spyglass with a book and a sculk sensor: it zooms like a spyglass, and reads what the player sees (GlancePanel). */
	public static final Item NATURALIST_SPYGLASS = register("naturalist_spyglass", SpyglassItem::new, new Item.Properties().stacksTo(1));
	/** A torch with a sculk sensor, held in hand: it marks the blocks where monsters can spawn (SpawnSpots). */
	public static final Item SCULK_TORCH = register("sculk_torch", Item::new, new Item.Properties().stacksTo(1));

	private CassonadeItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Cassonade.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> {
					output.insertAfter(Items.BUNDLE, BACKPACK);
					output.insertAfter(BACKPACK, ORGANIZED_BACKPACK);
					output.insertAfter(ORGANIZED_BACKPACK, BACKPACK_UPGRADE_TEMPLATE);
					output.insertAfter(Items.MAP, WORLD_MAP);
					output.insertAfter(Items.COMPASS, SCULK_COMPASS);
					output.insertAfter(Items.CLOCK, BAROMETER);
					output.insertAfter(Items.SPYGLASS, NATURALIST_SPYGLASS);
					output.insertAfter(NATURALIST_SPYGLASS, SCULK_TORCH);
				});
	}
}
