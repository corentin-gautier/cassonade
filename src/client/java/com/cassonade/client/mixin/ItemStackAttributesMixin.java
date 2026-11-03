package com.cassonade.client.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

import com.cassonade.client.DescriptionText;

/**
 * The attributes in a tooltip, in the enchantments' hierarchy: "When in Main Hand:" as their descriptions, small and dim, with
 * room above it instead of vanilla's blank line, each value in cream as their names, rather than vanilla's gray, dark green
 * and blue.
 */
@Mixin(ItemStack.class)
abstract class ItemStackAttributesMixin {
	@ModifyVariable(method = "addAttributeTooltips", at = @At("HEAD"), argsOnly = true)
	private Consumer<Component> cassonade$restyle(Consumer<Component> consumer) {
		return line -> {
			if (line == CommonComponents.EMPTY) {
				return;
			}
			if (line.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith("item.modifiers.")) {
				consumer.accept(line.copy().withStyle(style -> style.withColor(DescriptionText.COLOR).withInsertion(DescriptionText.HEADING)));
				return;
			}
			consumer.accept(cassonade$value(line).copy().withColor(DescriptionText.NAME_COLOR));
		};
	}

	/** A value with the player's base included comes as a space, then the value: aligned with the others instead. */
	@Unique
	private static Component cassonade$value(Component line) {
		return line.getContents() instanceof PlainTextContents plain && plain.text().isBlank() && line.getSiblings().size() == 1
				? line.getSiblings().getFirst()
				: line;
	}
}
