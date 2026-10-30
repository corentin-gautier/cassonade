package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;

/** No "Too Expensive!": the anvil takes any cost the player can pay (see AnvilScreenMixin for the label). */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	@Shadow
	@Final
	private DataSlot cost;
	@Shadow
	private boolean onlyRenaming;

	// The third 40 of createResult is the cap; the first is the price of a stack, the second the cap on renaming alone
	@ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 2))
	private int cassonade$noCostCap(int cap) {
		return Integer.MAX_VALUE;
	}

	@Inject(method = "createResult", at = @At("TAIL"))
	private void cassonade$freeRename(CallbackInfo ci) {
		if (onlyRenaming) {
			cost.set(0);
		}
	}

	// Vanilla refuses a result that costs nothing, which would leave a free rename stuck in its slot
	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void cassonade$takeFreeRename(Player player, boolean hasItem, CallbackInfoReturnable<Boolean> cir) {
		if (onlyRenaming && cost.get() == 0) {
			cir.setReturnValue(true);
		}
	}
}
