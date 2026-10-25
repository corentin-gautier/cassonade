package com.cassonade.client.mixin;

import java.util.HashSet;
import java.util.Set;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

import org.apache.commons.lang3.math.Fraction;

import com.cassonade.ServerSupport;
import com.cassonade.Cassonade;
import com.cassonade.client.InventorySounds;
import com.cassonade.client.CassonadeClient;
import com.cassonade.client.storage.StorageButtons;
import com.cassonade.storage.LockedSlots;
import com.cassonade.storage.StoragePayload;

/**
 * In survival, a middle click (the pick block key) on a slot sorts the inventory or the container it
 * belongs to. After a shift click on a stack, the stacks hovered while shift and the button stay held
 * move too, as if shift clicked one by one. Likewise, with a bundle carried, a left click on a stack
 * puts it in the bundle and a right click on an empty slot takes a stack out of the bundle there: the
 * stacks, or the empty slots, hovered while the button stays held follow. Clicks that move items make
 * a sound (InventorySounds). A click on one of the inventory's slots, the lock key held, locks or
 * unlocks it (LockedSlots): a padlock marks it, and a shift click or a sweep leaves its stack where it is.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	@Final
	protected AbstractContainerMenu menu;

	@Unique
	private static final Identifier PADLOCK = Cassonade.id("container/slot_lock");
	@Unique
	private static final int PADLOCK_WIDTH = 7;
	@Unique
	private static final int PADLOCK_HEIGHT = 8;

	/** The slots moved since the shift click that started the sweep; null when no sweep is going on. */
	@Unique
	private @Nullable Set<Slot> cassonade$swept;

	/** The slots clicked with the bundle carried since the click that started; null when not sweeping with one. */
	@Unique
	private @Nullable Set<Slot> cassonade$bundleSwept;
	/** Whether that sweep fills the bundle with the stacks, else empties it in the empty slots. */
	@Unique
	private boolean cassonade$filling;

	@Shadow
	@Final
	protected Set<Slot> quickCraftSlots;

	@Shadow
	private @Nullable Slot getHoveredSlot(double x, double y) {
		throw new AssertionError();
	}

	@Shadow
	protected abstract void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput);

	/**
	 * A click on a slot is no widget's drag. A button that rebuilt the screen as it was clicked (the
	 * inventory's tabs) is left focused and dragged when its release does not reach the screen, as with
	 * the recipe book's; Fabric's screen API then hands the next drag to it, and the sweeps never see it.
	 */
	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void cassonade$stopWidgetDrag(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (getHoveredSlot(event.x(), event.y()) != null) {
			((Screen) (Object) this).setDragging(false);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void cassonade$startShiftSweep(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		cassonade$swept = null;
		// The creative inventory's own shift click deletes or copies stacks: no sweep there
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !event.hasShiftDown() || !menu.getCarried().isEmpty()
				|| (Object) this instanceof CreativeModeInventoryScreen) {
			return;
		}
		Slot slot = getHoveredSlot(event.x(), event.y());
		if (slot != null && slot.hasItem()) {
			cassonade$swept = new HashSet<>();
			cassonade$swept.add(slot);
		}
	}

	/**
	 * The sweep goes on as the cursor is dragged, the button held: on the drag rather than each frame,
	 * since the screens with a recipe book draw themselves without the container screen's frame.
	 */
	@Inject(method = "mouseDragged", at = @At("HEAD"))
	private void cassonade$continueShiftSweep(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
		if (cassonade$swept == null) {
			return;
		}
		if (!Minecraft.getInstance().hasShiftDown() || !menu.getCarried().isEmpty()) {
			cassonade$swept = null;
			return;
		}
		Slot slot = getHoveredSlot(event.x(), event.y());
		if (slot != null && slot.hasItem() && cassonade$swept.add(slot)) {
			slotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);
		}
	}

	/** The sweep lasts as long as the button stays held. */
	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void cassonade$endSweeps(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			cassonade$swept = null;
		}
		if (cassonade$bundleSwept != null && event.button() == cassonade$bundleButton()) {
			cassonade$bundleSwept = null;
			// Vanilla would click again on release, with the bundle on the slot under the cursor
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void cassonade$startBundleSweep(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		cassonade$bundleSwept = null;
		boolean left = event.button() == InputConstants.MOUSE_BUTTON_LEFT && !event.hasShiftDown();
		if (!left && event.button() != InputConstants.MOUSE_BUTTON_RIGHT || (Object) this instanceof CreativeModeInventoryScreen) {
			return;
		}
		BundleContents contents = menu.getCarried().get(DataComponents.BUNDLE_CONTENTS);
		Slot slot = getHoveredSlot(event.x(), event.y());
		// A left click on an empty slot puts the bundle down, a right click on a stack puts the stack in it, as in vanilla
		if (contents == null || slot == null || slot.hasItem() != left) {
			return;
		}
		cassonade$filling = left;
		if (cassonade$canSweep()) {
			cassonade$bundleSwept = new HashSet<>();
			cassonade$bundleClick(slot);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void cassonade$continueBundleSweep(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
		if (cassonade$bundleSwept == null) {
			return;
		}
		Slot slot = getHoveredSlot(event.x(), event.y());
		if (slot != null && slot.hasItem() == cassonade$filling && cassonade$canSweep()) {
			cassonade$bundleClick(slot);
		}
		cir.setReturnValue(true);
	}

	@Unique
	private int cassonade$bundleButton() {
		return cassonade$filling ? InputConstants.MOUSE_BUTTON_LEFT : InputConstants.MOUSE_BUTTON_RIGHT;
	}

	/** Whether the bundle carried has room left to fill, or something left to empty. */
	@Unique
	private boolean cassonade$canSweep() {
		BundleContents contents = menu.getCarried().get(DataComponents.BUNDLE_CONTENTS);
		if (contents == null) {
			return false;
		}
		if (!cassonade$filling) {
			return !contents.isEmpty();
		}
		return contents.weight().result().map(weight -> weight.compareTo(Fraction.ONE) < 0).orElse(false);
	}

	/** A click on the slot with the bundle: the left one puts the slot's stack in it, the right one takes a stack out into the slot. */
	@Unique
	private void cassonade$bundleClick(Slot slot) {
		if (cassonade$bundleSwept.add(slot)) {
			slotClicked(slot, slot.index, cassonade$filling ? 0 : 1, ContainerInput.PICKUP);
		}
	}

	/** A tick for each slot a drag spreads the carried stack over, from the second, when it starts to split. */
	@Inject(method = "mouseDragged", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;recalculateQuickCraftRemaining()V"))
	private void cassonade$spreadSound(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
		if (quickCraftSlots.size() > 1) {
			InventorySounds.spread();
		}
	}

	/** The click's sound, from what it changed on the cursor and in the slot, as the client sees it at once. */
	@WrapOperation(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleContainerInput(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"))
	private void cassonade$sound(MultiPlayerGameMode gameMode, int containerId, int slotId, int button, ContainerInput input, Player player,
			Operation<Void> original) {
		Slot slot = slotId >= 0 && slotId < menu.slots.size() ? menu.getSlot(slotId) : null;
		ItemStack carriedBefore = menu.getCarried().copy();
		ItemStack slotBefore = slot != null ? slot.getItem().copy() : ItemStack.EMPTY;
		original.call(gameMode, containerId, slotId, button, input, player);
		InventorySounds.clicked(input, button, carriedBefore, slotBefore, menu.getCarried(), slot != null ? slot.getItem() : ItemStack.EMPTY);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void cassonade$lockSlot(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		// Kept by the server: not on one without the mod
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !cassonade$lockKeyDown() || !ServerSupport.onClient()
				|| (Object) this instanceof CreativeModeInventoryScreen) {
			return;
		}
		Slot hovered = getHoveredSlot(event.x(), event.y());
		if (hovered == null || !(hovered.container instanceof Inventory inventory) || hovered.getContainerSlot() >= Inventory.INVENTORY_SIZE) {
			return;
		}
		int slot = hovered.getContainerSlot();
		ClientPlayNetworking.send(new LockedSlots.Toggle(slot));
		InventorySounds.locked(!LockedSlots.of(inventory.player).isLocked(slot));
		cir.setReturnValue(true);
	}

	/** Screens take the keys from the key mappings: whether the lock key's own key is held. */
	@Unique
	private static boolean cassonade$lockKeyDown() {
		InputConstants.Key key = ((KeyMappingAccessor) CassonadeClient.LOCK_SLOT).cassonade$getKey();
		return key.getType() == InputConstants.Type.KEYBOARD && InputConstants.isKeyDown(key.getValue());
	}

	/** A shift click, or a sweep, passes over a locked slot. */
	@Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
			at = @At("HEAD"), cancellable = true)
	private void cassonade$keepLocked(Slot slot, int slotId, int buttonNum, ContainerInput input, CallbackInfo ci) {
		if (input == ContainerInput.QUICK_MOVE && slot != null && LockedSlots.isLocked(slot)) {
			ci.cancel();
		}
	}

	/** The padlock over a locked slot, in its top left corner, over the item. */
	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void cassonade$padlock(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (LockedSlots.isLocked(slot)) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PADLOCK, slot.x - 1, slot.y - 1, PADLOCK_WIDTH, PADLOCK_HEIGHT);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void cassonade$sortOnMiddleClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
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
			InventorySounds.stored();
			cir.setReturnValue(true);
		}
	}
}
