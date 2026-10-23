package com.vanillasugar.client.mixin;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import com.vanillasugar.client.storage.StorageButtons;
import com.vanillasugar.storage.StoragePayload;

/**
 * In survival, a middle click (the pick block key) on a slot sorts the inventory or the container it
 * belongs to. After a shift click on a stack, the stacks hovered while shift stays held move too, as
 * if shift clicked one by one.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	@Final
	protected AbstractContainerMenu menu;

	/** The slots moved since the shift click that started the sweep; null when no sweep is going on. */
	@Unique
	private @Nullable Set<Slot> vanillasugar$swept;

	@Shadow
	private @Nullable Slot getHoveredSlot(double x, double y) {
		throw new AssertionError();
	}

	@Shadow
	protected abstract void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput);

	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void vanillasugar$startShiftSweep(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		vanillasugar$swept = null;
		// The creative inventory's own shift click deletes or copies stacks: no sweep there
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !event.hasShiftDown() || !menu.getCarried().isEmpty()
				|| (Object) this instanceof CreativeModeInventoryScreen) {
			return;
		}
		Slot slot = getHoveredSlot(event.x(), event.y());
		if (slot != null && slot.hasItem()) {
			vanillasugar$swept = new HashSet<>();
			vanillasugar$swept.add(slot);
		}
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void vanillasugar$continueShiftSweep(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (vanillasugar$swept == null) {
			return;
		}
		if (!Minecraft.getInstance().hasShiftDown() || !menu.getCarried().isEmpty()) {
			vanillasugar$swept = null;
			return;
		}
		Slot slot = getHoveredSlot(mouseX, mouseY);
		if (slot != null && slot.hasItem() && vanillasugar$swept.add(slot)) {
			slotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);
		}
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
