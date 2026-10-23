package com.vanillasugar.hopper;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import com.vanillasugar.VanillaSugar;

/**
 * Every hopper has a filter slot: once an item is in it, the hopper only takes in that item, from the container above it,
 * the items dropped on it and the hoppers that feed it. The filter is attached to the hopper's block entity, which
 * HopperBlockEntityMixin applies, and the block state's FILTERED property, added by HopperBlockMixin, gives a filtering
 * hopper its own look.
 */
public final class HopperFilter {
	public static final BooleanProperty FILTERED = BooleanProperty.create("filtered");
	public static final MenuType<HopperFilterMenu> MENU = Registry.register(BuiltInRegistries.MENU, VanillaSugar.id("hopper"),
			new MenuType<>(HopperFilterMenu::new, FeatureFlags.VANILLA_SET));
	private static final AttachmentType<ItemStack> FILTER = AttachmentRegistry.create(VanillaSugar.id("filter"), builder -> builder
			.persistent(ItemStack.CODEC));

	private HopperFilter() {
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

		Level level = hopper.getLevel();
		BlockState state = hopper.getBlockState();
		if (level != null && !level.isClientSide() && state.hasProperty(FILTERED) && state.getValue(FILTERED) == filter.isEmpty()) {
			level.setBlock(hopper.getBlockPos(), state.setValue(FILTERED, !filter.isEmpty()), Block.UPDATE_CLIENTS);
		}
	}

	/** Whether the filter lets the stack through: an empty filter lets everything through, like a hopper. */
	public static boolean accepts(ItemStack filter, ItemStack stack) {
		return filter.isEmpty() || ItemStack.isSameItem(filter, stack);
	}

	/** Loads the class, which registers the menu. */
	public static void init() {
	}
}
