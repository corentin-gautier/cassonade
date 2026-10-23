package com.vanillasugar.mixin;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import com.vanillasugar.gear.ElytraGear;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	/** Mending repairs the elytra of the gear slot as if it were worn as a chestplate. */
	@Inject(method = "getRandomItemWith", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/Util;getRandomSafe(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/util/Optional;"))
	private static void vanillasugar$includeGearElytra(DataComponentType<?> componentType, LivingEntity source, Predicate<ItemStack> predicate,
			CallbackInfoReturnable<Optional<EnchantedItemInUse>> cir, @Local(name = "items") List<EnchantedItemInUse> items) {
		if (!(source instanceof Player player)) {
			return;
		}

		ItemStack elytra = ElytraGear.worn(player);
		if (elytra.isEmpty() || !predicate.test(elytra)) {
			return;
		}
		for (Holder<Enchantment> enchantment : elytra.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).keySet()) {
			if (enchantment.value().effects().has(componentType) && enchantment.value().matchingSlot(EquipmentSlot.CHEST)) {
				items.add(new EnchantedItemInUse(elytra, EquipmentSlot.CHEST, source));
			}
		}
	}
}
