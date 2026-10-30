package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;

import com.cassonade.gear.BackpackContainer;
import com.cassonade.gear.OrganizedBackpack;

/** The recipe book counts what is in an organized backpack, on the client to show it and on the server to place it. */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
	@Shadow
	@Final
	public Player player;

	@Inject(method = "fillStackedContents", at = @At("TAIL"))
	private void cassonade$countBackpack(StackedItemContents contents, CallbackInfo info) {
		BackpackContainer backpack = OrganizedBackpack.of(player);
		if (backpack != null) {
			for (int i = 0; i < backpack.getContainerSize(); i++) {
				contents.accountSimpleStack(backpack.getItem(i));
			}
		}
	}
}
