package com.cassonade.client.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * What each enchantment does, under its name: always on an enchanted book, where you choose one, and on
 * enchanted gear while Shift is held, so a sword's tooltip stays short. Enchantments without a description
 * in the language files (from data packs or other mods) show their name alone.
 */
@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentsMixin {
	/** Dimmer than the enchantments' gray names, warm like the quest tracker's hint. */
	@Unique
	private static final int DESCRIPTION = 0xFF9A8782;
	/** Past this, a description goes on to the next line, so the tooltip keeps a reasonable width. */
	@Unique
	private static final int WIDTH = 180;

	@ModifyVariable(method = "addToTooltip", at = @At("HEAD"), argsOnly = true)
	private Consumer<Component> cassonade$describe(Consumer<Component> consumer, Item.TooltipContext context, Consumer<Component> unused,
			TooltipFlag flag, DataComponentGetter components) {
		ItemEnchantments self = (ItemEnchantments) (Object) this;
		boolean book = components.get(DataComponents.STORED_ENCHANTMENTS) == self;
		if (!book && (!RenderSystem.isOnRenderThread() || !Minecraft.getInstance().hasShiftDown())) {
			return consumer;
		}
		return line -> {
			consumer.accept(line);
			// The line is the enchantment's name, with its level: find which one it names
			for (Holder<Enchantment> enchantment : self.keySet()) {
				if (enchantment.value().description().getContents().equals(line.getContents())) {
					enchantment.unwrapKey().ifPresent(key -> cassonade$addDescription(consumer, key.identifier()));
					return;
				}
			}
		};
	}

	@Unique
	private static void cassonade$addDescription(Consumer<Component> consumer, Identifier id) {
		String key = "gui.cassonade.enchantment." + id.getNamespace() + "." + id.getPath();
		if (!Language.getInstance().has(key)) {
			return;
		}
		Style style = Style.EMPTY.withColor(DESCRIPTION);
		MutableComponent description = Component.translatable(key).withStyle(style);
		if (!RenderSystem.isOnRenderThread()) {
			consumer.accept(description);
			return;
		}
		for (FormattedText part : Minecraft.getInstance().font.getSplitter().splitLines(description, WIDTH, style)) {
			consumer.accept(Component.literal(part.getString()).withStyle(style));
		}
	}
}
