package com.vanillasugar.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import com.vanillasugar.client.storage.StorageButtons;
import com.vanillasugar.storage.StoragePayload;

/** In survival, a middle click (the pick block key) on a slot sorts the inventory or the container it belongs to. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	@Final
	protected AbstractContainerMenu menu;

	@Shadow
	private @Nullable Slot getHoveredSlot(double x, double y) {
		throw new AssertionError();
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void vanillasugar$sortOnMiddleClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		Minecraft minecraft = Minecraft.getInstance();
		// In creative, the middle click copies the stack
		if (!minecraft.options.keyPickItem.matchesMouse(event) || minecraft.player.hasInfiniteMaterials() || !menu.getCarried().isEmpty()) {
			return;
		}
		Slot slot = getHoveredSlot(event.x(), event.y());
		if (slot == null) {
			return;
		}
		StoragePayload.Action action = StorageButtons.sortFor(menu, slot);
		if (action != null) {
			ClientPlayNetworking.send(new StoragePayload(action));
			cir.setReturnValue(true);
		}
	}
}
