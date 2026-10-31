package com.cassonade.client.mixin;

import java.util.List;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import com.cassonade.client.DescriptionText;
import com.cassonade.client.map.SmallText;

/**
 * What each enchantment does, under its name: always on an enchanted book, where you choose one, and on
 * enchanted gear while Shift is held, so a sword's tooltip stays short. Enchantments without a description
 * in the language files (from data packs or other mods) show their name alone.
 */
@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentsMixin {
	/** Past this, a description goes on to the next line, so the tooltip keeps a reasonable width. */
	@Unique
	private static final int WIDTH = 180;

	@ModifyVariable(method = "addToTooltip", at = @At("HEAD"), argsOnly = true)
	private Consumer<Component> cassonade$describe(Consumer<Component> consumer, Item.TooltipContext context, Consumer<Component> unused,
			TooltipFlag flag, DataComponentGetter components) {
		ItemEnchantments self = (ItemEnchantments) (Object) this;
		boolean book = components.get(DataComponents.STORED_ENCHANTMENTS) == self;
		if (!book && (!RenderSystem.isOnRenderThread() || !Minecraft.getInstance().hasShiftDown())) {
			return line -> consumer.accept(cassonade$brighten(line));
		}
		int[] named = {0};
		return line -> {
			consumer.accept(cassonade$brighten(line));
			// The line is the enchantment's name, with its level: find which one it names
			for (Holder<Enchantment> enchantment : self.keySet()) {
				if (enchantment.value().description().getContents().equals(line.getContents())) {
					// Room under a description only if another enchantment follows
					boolean more = ++named[0] < self.size();
					enchantment.unwrapKey().ifPresent(key -> cassonade$addDescription(consumer, key.identifier(), more));
					return;
				}
			}
		};
	}

	/** An enchantment's name in cream rather than gray, to stand out from its description; a curse stays red. */
	@Unique
	private static Component cassonade$brighten(Component name) {
		return TextColor.fromLegacyFormat(ChatFormatting.GRAY).equals(name.getStyle().getColor())
				? name.copy().withColor(DescriptionText.NAME_COLOR)
				: name;
	}

	@Unique
	private static void cassonade$addDescription(Consumer<Component> consumer, Identifier id, boolean spaced) {
		String key = "gui.cassonade.enchantment." + id.getNamespace() + "." + id.getPath();
		if (!Language.getInstance().has(key)) {
			return;
		}
		Style style = Style.EMPTY.withColor(DescriptionText.COLOR).withInsertion(DescriptionText.DESCRIPTION);
		MutableComponent description = Component.translatable(key).withStyle(style);
		if (!RenderSystem.isOnRenderThread()) {
			consumer.accept(description);
			return;
		}
		// The text is drawn smaller (ClientTextTooltipMixin): split at the width it would have at full size
		int width = Math.round(WIDTH / SmallText.scale());
		List<FormattedText> parts = Minecraft.getInstance().font.getSplitter().splitLines(description, width, style);
		for (int i = 0; i < parts.size(); i++) {
			Style partStyle = spaced && i == parts.size() - 1 ? style.withInsertion(DescriptionText.LAST) : style;
			consumer.accept(Component.literal(parts.get(i).getString()).withStyle(partStyle));
		}
	}
}
