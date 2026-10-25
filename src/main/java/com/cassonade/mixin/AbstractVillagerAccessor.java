package com.cassonade.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;

/** A villager's trades as they are, without generating them as getOffers() does for one never traded with. */
@Mixin(AbstractVillager.class)
public interface AbstractVillagerAccessor {
	@Accessor("offers")
	@Nullable MerchantOffers cassonade$getOffers();
}
