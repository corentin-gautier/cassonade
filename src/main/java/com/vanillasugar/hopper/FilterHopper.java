package com.vanillasugar.hopper;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.vanillasugar.VanillaSugar;

/**
 * A hopper that only takes in the item of its filter slot: from the container above it, the items dropped on it and the
 * hoppers that feed it. In every other way it is a vanilla hopper, down to its block entity (the filter is attached to it,
 * and HopperBlockEntityMixin applies it): it moves items at the same pace, redstone locks it and comparators read it the
 * same way.
 */
public final class FilterHopper {
	public static final Block BLOCK = registerBlock("filter_hopper");
	public static final MenuType<FilterHopperMenu> MENU = Registry.register(BuiltInRegistries.MENU, VanillaSugar.id("filter_hopper"),
			new MenuType<>(FilterHopperMenu::new, FeatureFlags.VANILLA_SET));
	private static final AttachmentType<ItemStack> FILTER = AttachmentRegistry.create(VanillaSugar.id("filter"), builder -> builder
			.persistent(ItemStack.CODEC));

	private FilterHopper() {
	}

	private static Block registerBlock(String name) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, VanillaSugar.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, new HopperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER).setId(key)));
	}

	public static ItemStack filter(BlockEntity hopper) {
		return hopper.getAttachedOrElse(FILTER, ItemStack.EMPTY);
	}

	public static void setFilter(BlockEntity hopper, ItemStack filter) {
		if (filter.isEmpty()) {
			hopper.removeAttached(FILTER);
		} else {
			hopper.setAttached(FILTER, filter);
		}
	}

	/** Whether the filter lets the stack through: an empty filter lets everything through, like a hopper. */
	public static boolean accepts(ItemStack filter, ItemStack stack) {
		return filter.isEmpty() || ItemStack.isSameItem(filter, stack);
	}

	public static void init() {
		BlockEntityTypes.HOPPER.addValidBlock(BLOCK);
	}
}
