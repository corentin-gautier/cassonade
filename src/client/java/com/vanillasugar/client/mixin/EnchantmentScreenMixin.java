package com.vanillasugar.client.mixin;

import java.util.ArrayList;
import java.util.List;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import com.vanillasugar.client.EnchantmentPreview;

/**
 * The enchanting table's tooltip lists every enchantment an option gives, not just one with a "...?".
 * The client works the list out the way the server does, from the seed it sends (see
 * EnchantmentSeedPayload), the costs and the item; the enchantments' registry and tags come with the world.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
	private EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"))
	private List<Component> vanillasugar$listAllEnchantments(List<Component> texts, @Local(name = "i") int option) {
		List<EnchantmentInstance> enchantments = EnchantmentPreview.of(minecraft, menu, option);
		// Without the seed from the server, the vanilla clue
		if (enchantments.isEmpty()) {
			return texts;
		}

		// In place of the clue, the first line
		List<Component> lines = new ArrayList<>();
		for (EnchantmentInstance enchantment : enchantments) {
			lines.add(Enchantment.getFullname(enchantment.enchantment(), enchantment.level()).copy().withStyle(ChatFormatting.WHITE));
		}
		lines.addAll(texts.subList(1, texts.size()));
		return lines;
	}
}
