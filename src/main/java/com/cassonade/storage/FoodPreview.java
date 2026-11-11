package com.cassonade.storage;

import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/** Food shows in its tooltip the hunger it restores and its saturation, as drumsticks. */
public record FoodPreview(FoodProperties food) implements TooltipComponent {
	public static Optional<FoodPreview> of(ItemStack stack) {
		if (!Settings.on(Setting.FOOD_TOOLTIP)) {
			return Optional.empty();
		}
		return Optional.ofNullable(stack.get(DataComponents.FOOD)).filter(food -> food.nutrition() > 0).map(FoodPreview::new);
	}
}
