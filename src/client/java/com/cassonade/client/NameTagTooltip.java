package com.cassonade.client;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import com.cassonade.ServerSupport;

/** What a blank name tag does beyond naming, which nothing else in the game would tell. */
public final class NameTagTooltip {
	private NameTagTooltip() {
	}

	public static void append(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
		if (stack.is(Items.NAME_TAG) && !stack.has(DataComponents.CUSTOM_NAME) && ServerSupport.onClient()) {
			lines.add(DescriptionText.of(Component.translatable("gui.cassonade.name_tag.hint")));
		}
	}
}
