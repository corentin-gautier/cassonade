package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

import com.cassonade.client.InventorySounds;
import com.cassonade.storage.LockedSlots;

/** The drop key leaves the stack of a locked hotbar slot in hand, with the padlock's clink to say why. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	@Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
	private void cassonade$keepLocked(LocalPlayer player, boolean all, CallbackInfo ci) {
		if (LockedSlots.of(player).isLocked(player.getInventory().getSelectedSlot())) {
			InventorySounds.locked(true);
			ci.cancel();
		}
	}
}
