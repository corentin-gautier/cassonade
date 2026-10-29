package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;

@Mixin(MerchantMenu.class)
public interface MerchantMenuAccessor {
	@Accessor("trader")
	Merchant cassonade$getTrader();
}
