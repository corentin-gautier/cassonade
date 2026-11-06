package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;

import com.cassonade.enchanting.EnchantmentReroll;

/** The table's fourth button draws other offers, see {@link EnchantmentReroll}. */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
	@Shadow
	@Final
	private Container enchantSlots;
	@Shadow
	@Final
	private ContainerLevelAccess access;
	@Shadow
	@Final
	private DataSlot enchantmentSeed;

	@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
	private void cassonade$reroll(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
		if (buttonId != EnchantmentReroll.BUTTON) {
			return;
		}
		EnchantmentMenu self = (EnchantmentMenu) (Object) this;
		if (!EnchantmentReroll.canReroll(player, self)) {
			cir.setReturnValue(false);
			return;
		}
		EnchantmentReroll.pay(player);
		// As after enchanting: the new seed drawn, the offers worked out from it
		enchantmentSeed.set(player.getEnchantmentSeed());
		self.slotsChanged(enchantSlots);
		access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F,
				level.getRandom().nextFloat() * 0.1F + 0.9F));
		cir.setReturnValue(true);
	}
}
