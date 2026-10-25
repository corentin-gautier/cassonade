package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.cassonade.ServerSupport;
import com.cassonade.gear.BackpackContainer;
import com.cassonade.gear.BackpackMenu;
import com.cassonade.gear.BackpackMenuSlot;
import com.cassonade.gear.GearContainer;
import com.cassonade.gear.GearLayout;
import com.cassonade.gear.GearMenuSlot;
import com.cassonade.gear.GearSlot;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractCraftingMenu implements BackpackMenu {
	private BackpackContainer cassonade$backpack;

	private InventoryMenuMixin(MenuType<?> menuType, int containerId, int width, int height) {
		super(menuType, containerId, width, height);
	}

	@Override
	public BackpackContainer cassonade$backpack() {
		return cassonade$backpack;
	}

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/InventoryMenu;addResultSlot(Lnet/minecraft/world/entity/player/Player;II)Lnet/minecraft/world/inventory/Slot;"))
	private Slot cassonade$moveResultSlot(InventoryMenu menu, Player player, int x, int y, Operation<Slot> original) {
		return original.call(menu, player, GearLayout.RESULT_X, GearLayout.RESULT_Y);
	}

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/InventoryMenu;addCraftingGridSlots(II)V"))
	private void cassonade$moveCraftingGrid(InventoryMenu menu, int left, int top, Operation<Void> original) {
		original.call(menu, GearLayout.CRAFTING_X, GearLayout.CRAFTING_Y);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void cassonade$addGearSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
		GearContainer gear = new GearContainer(owner);
		for (GearSlot slot : GearSlot.values()) {
			addSlot(new GearMenuSlot(gear, slot));
		}

		cassonade$backpack = new BackpackContainer(owner);
		for (int i = 0; i < BackpackContainer.SIZE; i++) {
			addSlot(new BackpackMenuSlot(cassonade$backpack, i));
		}
	}

	/**
	 * Shift-click: gear and backpack go back to the inventory; from the inventory, gear items are
	 * equipped and, while the backpack panel is open, everything else goes into it. Armor and offhand
	 * keep vanilla's priority, and anything we cannot place falls through to vanilla.
	 */
	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void cassonade$quickMoveGear(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		if (!ServerSupport.has(player)) {
			return;
		}
		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) {
			return;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex >= GearLayout.GEAR_START) {
			if (!moveItemStackTo(stack, InventoryMenu.INV_SLOT_START, InventoryMenu.USE_ROW_SLOT_END, false)) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}
		} else if (slotIndex >= InventoryMenu.INV_SLOT_START && slotIndex < InventoryMenu.USE_ROW_SLOT_END) {
			if (!cassonade$moveFromInventory(player, stack)) {
				return;
			}
		} else {
			return;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY, original);
		} else {
			slot.setChanged();
		}
		slot.onTake(player, stack);
		cir.setReturnValue(stack.getCount() == original.getCount() ? ItemStack.EMPTY : original);
	}

	private boolean cassonade$moveFromInventory(Player player, ItemStack stack) {
		GearSlot gearSlot = GearSlot.forItem(stack);
		if (gearSlot != null) {
			int index = GearLayout.GEAR_START + gearSlot.ordinal();
			if (!slots.get(index).hasItem() && moveItemStackTo(stack, index, index + 1, false)) {
				return true;
			}
		}

		if (!cassonade$backpack.isOpen() || cassonade$prefersVanillaEquip(player, stack)) {
			return false;
		}
		return moveItemStackTo(stack, GearLayout.BACKPACK_START, GearLayout.BACKPACK_END, false);
	}

	private boolean cassonade$prefersVanillaEquip(Player player, ItemStack stack) {
		EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(stack);
		if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
			return !slots.get(8 - equipmentSlot.getIndex()).hasItem();
		}
		return equipmentSlot == EquipmentSlot.OFFHAND && !slots.get(InventoryMenu.SHIELD_SLOT).hasItem();
	}
}
