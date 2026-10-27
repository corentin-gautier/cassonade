package com.cassonade.client;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.cassonade.furnace.FuelTimes;

/**
 * How many items a fuel smelts, the last line of its tooltip while a furnace, blast furnace or smoker is
 * open: that is where the choice of fuel is made, and elsewhere it would weigh down every plank and stick.
 * The burn times come from the server (FuelTimes): on a server without the mod, nothing shows.
 */
public final class FuelTooltip {
	/** The enchantments' descriptions' warm gray, like the anvil's penalty (RepairCostTooltip). */
	private static final int COLOR = 0xFF9A8782;
	private static final Map<Item, Integer> TICKS = new HashMap<>();

	private FuelTooltip() {
	}

	public static void receive(FuelTimes times) {
		TICKS.clear();
		times.ticks().forEach((item, ticks) -> TICKS.put(item.value(), ticks));
	}

	public static void clear() {
		TICKS.clear();
	}

	/** Items a piece of this fuel smelts in a furnace, 0 if it is no fuel or the server didn't say. */
	public static double itemsSmelted(ItemStack stack) {
		return TICKS.getOrDefault(stack.getItem(), 0) / (double) FuelTimes.TICKS_PER_ITEM;
	}

	public static void append(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
		if (!(Minecraft.getInstance().gui.screen() instanceof AbstractFurnaceScreen<?>)) {
			return;
		}
		double items = itemsSmelted(stack);
		if (items > 0) {
			String key = items == 1 ? "gui.cassonade.fuel.one" : "gui.cassonade.fuel";
			lines.add(Component.translatable(key, format(items)).withColor(COLOR));
		}
	}

	/** Whole numbers as they are, others to one decimal: a plank smelts 1.5 items, a stick 0.5. */
	static String format(double items) {
		return items == Math.floor(items) ? Integer.toString((int) items) : String.format(Locale.ROOT, "%.1f", items);
	}
}
