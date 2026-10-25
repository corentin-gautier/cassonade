package com.cassonade.storage;

import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;

/**
 * A filled map shows what it holds in its tooltip, drawn in the minimap's frame, with the game's lines about
 * it (its id, locked) written small underneath rather than in the tooltip's text.
 */
public record MapPreview(MapId id, ItemStack stack) implements TooltipComponent {
	public static Optional<MapPreview> of(ItemStack stack) {
		return Optional.ofNullable(stack.get(DataComponents.MAP_ID)).map(id -> new MapPreview(id, stack));
	}
}
