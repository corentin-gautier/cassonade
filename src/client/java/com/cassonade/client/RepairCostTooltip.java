package com.cassonade.client;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The levels an anvil adds to every use on an item already worked on there: the game keeps this penalty on
 * the item and doubles it, plus one, each time. The last line of the tooltip, only once there is one.
 * Client only: the penalty travels with the item, so it works on any server.
 */
public final class RepairCostTooltip {
	private RepairCostTooltip() {
	}

	public static void append(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
		int cost = stack.getOrDefault(DataComponents.REPAIR_COST, 0);
		if (cost > 0) {
			String key = cost == 1 ? "gui.cassonade.repair_cost.one" : "gui.cassonade.repair_cost";
			lines.add(Component.translatable(key, cost).withStyle(style -> style.withColor(DescriptionText.COLOR).withInsertion(DescriptionText.SPACED)));
		}
	}
}
