package com.cassonade.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

/**
 * Sneaking, an empty hand on an armor stand swaps the player's whole armor with the stand's, piece by
 * piece: a stand by the door holds the mining set while the diamond set goes out. A piece with Curse of
 * Binding stays on the player, and a slot the stand has locked (its DisabledSlots) is left as it is.
 */
@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {
	@Unique
	private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	@Shadow
	private int disabledSlots;

	@Shadow
	public abstract boolean isMarker();

	@Shadow
	private boolean isDisabled(EquipmentSlot slot) {
		throw new AssertionError();
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void cassonade$swapArmor(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
		if (isMarker() || player.isSpectator() || !player.isShiftKeyDown() || hand != InteractionHand.MAIN_HAND
				|| !player.getMainHandItem().isEmpty()) {
			return;
		}
		if (player.level().isClientSide()) {
			cir.setReturnValue(InteractionResult.SUCCESS);
			return;
		}
		ArmorStand stand = (ArmorStand) (Object) this;
		boolean swapped = false;
		for (EquipmentSlot slot : ARMOR) {
			ItemStack worn = player.getItemBySlot(slot);
			ItemStack shown = stand.getItemBySlot(slot);
			if (worn.isEmpty() && shown.isEmpty() || !cassonade$canSwap(slot, shown)
					|| !player.isCreative() && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
				continue;
			}
			stand.setItemSlot(slot, worn);
			player.setItemSlot(slot, shown);
			swapped = true;
		}
		if (swapped) {
			cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
		}
	}

	/** The stand's own locks, as ArmorStand.swapItem reads them: one bit against taking, one against placing. */
	@Unique
	private boolean cassonade$canSwap(EquipmentSlot slot, ItemStack shown) {
		if (isDisabled(slot)) {
			return false;
		}
		int lock = shown.isEmpty() ? slot.getFilterBit(16) : slot.getFilterBit(8);
		return (disabledSlots & 1 << lock) == 0;
	}
}
