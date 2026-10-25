package com.vanillasugar.client.mixin;

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

import com.vanillasugar.ServerSupport;
import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.InventorySounds;
import com.vanillasugar.client.VanillaSugarClient;
import com.vanillasugar.client.storage.StorageButtons;
import com.vanillasugar.storage.LockedSlots;
import com.vanillasugar.storage.StoragePayload;

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
	private static final Identifier PADLOCK = VanillaSugar.id("container/slot_lock");
	@Unique
	private static final int PADLOCK_WIDTH = 7;
	@Unique
	private static final int PADLOCK_HEIGHT = 8;

	/** The slots moved since the shift click that started the sweep; null when no sweep is going on. */
	@Unique
	private @Nullable Set<Slot> vanillasugar$swept;

	/** The slots clicked with the bundle carried since the click that started; null when not sweeping with one. */
	@Unique
	private @Nullable Set<Slot> vanillasugar$bundleSwept;
	/** Whether that sweep fills the bundle with the stacks, else empties it in the empty slots. */
	@Unique
	private boolean vanillasugar$filling;

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
	private void vanillasugar$stopWidgetDrag(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (getHoveredSlot(event.x(), event.y()) != null) {
			((Screen) (Object) this).setDragging(false);
		}
	}

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

	/**
	 * The sweep goes on as the cursor is dragged, the button held: on the drag rather than each frame,
	 * since the screens with a recipe book draw themselves without the container screen's frame.
	 */
	@Inject(method = "mouseDragged", at = @At("HEAD"))
	private void vanillasugar$continueShiftSweep(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
		if (vanillasugar$swept == null) {
			return;
		}
		if (!Minecraft.getInstance().hasShiftDown() || !menu.getCarried().isEmpty()) {
			vanillasugar$swept = null;
			return;
		}
		Slot slot = getHoveredSlot(event.x(), event.y());
		if (slot != null && slot.hasItem() && vanillasugar$swept.add(slot)) {
			slotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);
		}
	}

	/** The sweep lasts as long as the button stays held. */
	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void vanillasugar$endSweeps(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			vanillasugar$swept = null;
		}
		if (vanillasugar$bundleSwept != null && event.button() == vanillasugar$bundleButton()) {
			vanillasugar$bundleSwept = null;
			// Vanilla would click again on release, with the bundle on the slot under the cursor
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void vanillasugar$startBundleSweep(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		vanillasugar$bundleSwept = null;
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
		vanillasugar$filling = left;
		if (vanillasugar$canSweep()) {
			vanillasugar$bundleSwept = new HashSet<>();
			vanillasugar$bundleClick(slot);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void vanillasugar$continueBundleSweep(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
		if (vanillasugar$bundleSwept == null) {
			return;
		}
		Slot slot = getHoveredSlot(event.x(), event.y());
		if (slot != null && slot.hasItem() == vanillasugar$filling && vanillasugar$canSweep()) {
			vanillasugar$bundleClick(slot);
		}
		cir.setReturnValue(true);
	}

	@Unique
	private int vanillasugar$bundleButton() {
		return vanillasugar$filling ? InputConstants.MOUSE_BUTTON_LEFT : InputConstants.MOUSE_BUTTON_RIGHT;
	}

	/** Whether the bundle carried has room left to fill, or something left to empty. */
	@Unique
	private boolean vanillasugar$canSweep() {
		BundleContents contents = menu.getCarried().get(DataComponents.BUNDLE_CONTENTS);
		if (contents == null) {
			return false;
		}
		if (!vanillasugar$filling) {
			return !contents.isEmpty();
		}
		return contents.weight().result().map(weight -> weight.compareTo(Fraction.ONE) < 0).orElse(false);
	}

	/** A click on the slot with the bundle: the left one puts the slot's stack in it, the right one takes a stack out into the slot. */
	@Unique
	private void vanillasugar$bundleClick(Slot slot) {
		if (vanillasugar$bundleSwept.add(slot)) {
			slotClicked(slot, slot.index, vanillasugar$filling ? 0 : 1, ContainerInput.PICKUP);
		}
	}

	/** A tick for each slot a drag spreads the carried stack over, from the second, when it starts to split. */
	@Inject(method = "mouseDragged", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;recalculateQuickCraftRemaining()V"))
	private void vanillasugar$spreadSound(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
		if (quickCraftSlots.size() > 1) {
			InventorySounds.spread();
		}
	}

	/** The click's sound, from what it changed on the cursor and in the slot, as the client sees it at once. */
	@WrapOperation(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleContainerInput(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"))
	private void vanillasugar$sound(MultiPlayerGameMode gameMode, int containerId, int slotId, int button, ContainerInput input, Player player,
			Operation<Void> original) {
		Slot slot = slotId >= 0 && slotId < menu.slots.size() ? menu.getSlot(slotId) : null;
		ItemStack carriedBefore = menu.getCarried().copy();
		ItemStack slotBefore = slot != null ? slot.getItem().copy() : ItemStack.EMPTY;
		original.call(gameMode, containerId, slotId, button, input, player);
		InventorySounds.clicked(input, button, carriedBefore, slotBefore, menu.getCarried(), slot != null ? slot.getItem() : ItemStack.EMPTY);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void vanillasugar$lockSlot(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		// Kept by the server: not on one without the mod
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !vanillasugar$lockKeyDown() || !ServerSupport.onClient()
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
	private static boolean vanillasugar$lockKeyDown() {
		InputConstants.Key key = ((KeyMappingAccessor) VanillaSugarClient.LOCK_SLOT).vanillasugar$getKey();
		return key.getType() == InputConstants.Type.KEYBOARD && InputConstants.isKeyDown(key.getValue());
	}

	/** A shift click, or a sweep, passes over a locked slot. */
	@Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
			at = @At("HEAD"), cancellable = true)
	private void vanillasugar$keepLocked(Slot slot, int slotId, int buttonNum, ContainerInput input, CallbackInfo ci) {
		if (input == ContainerInput.QUICK_MOVE && slot != null && LockedSlots.isLocked(slot)) {
			ci.cancel();
		}
	}

	/** The padlock over a locked slot, in its top left corner, over the item. */
	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void vanillasugar$padlock(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (LockedSlots.isLocked(slot)) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PADLOCK, slot.x - 1, slot.y - 1, PADLOCK_WIDTH, PADLOCK_HEIGHT);
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
			InventorySounds.stored();
			cir.setReturnValue(true);
		}
	}
}
