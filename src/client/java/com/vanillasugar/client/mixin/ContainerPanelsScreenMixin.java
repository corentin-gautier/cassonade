package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.vanillasugar.client.ContainerPanels;

/** The window grows by the room left around the line between its two parts, the inventory's label follows its slots (ContainerPanels). */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerPanelsScreenMixin {
	@Shadow
	@Final
	@Mutable
	protected int imageHeight;

	@Shadow
	protected int inventoryLabelY;

	@Inject(method = "<init>(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;II)V",
			at = @At("RETURN"))
	private void vanillasugar$makeRoom(AbstractContainerMenu menu, Inventory inventory, Component title, int width, int height, CallbackInfo ci) {
		int shift = ContainerPanels.shift(menu);
		imageHeight += shift;
		// The chest's screen sets its label again from the height, right after: it lands at the same place
		inventoryLabelY += shift;
	}
}
