package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

import com.vanillasugar.gear.GearLayout;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
	/** The creative inventory tab mirrors every slot of the survival inventory; keep ours off-screen there. */
	@ModifyArgs(method = "selectTab", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
	private void vanillasugar$hideGearSlots(Args args) {
		int index = args.get(1);
		if (index >= GearLayout.GEAR_START) {
			args.set(2, -2000);
			args.set(3, -2000);
		}
	}
}
