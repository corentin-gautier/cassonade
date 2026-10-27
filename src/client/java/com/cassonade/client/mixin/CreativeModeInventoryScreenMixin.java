package com.cassonade.client.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.Identifier;

import com.cassonade.gear.GearLayout;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
	@Shadow
	@Final
	private static Identifier[] UNSELECTED_TOP_TABS;
	@Shadow
	@Final
	private static Identifier[] SELECTED_TOP_TABS;
	@Shadow
	@Final
	private static Identifier[] UNSELECTED_BOTTOM_TABS;
	@Shadow
	@Final
	private static Identifier[] SELECTED_BOTTOM_TABS;

	/**
	 * The tabs of a row all look alike, as in vanilla: one sprite for those not selected, one for the selected,
	 * the first and the last selected apart, as they join the window's corners. The other files are not drawn.
	 */
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void cassonade$sharedTabs(CallbackInfo ci) {
		for (Identifier[] tabs : new Identifier[][] {UNSELECTED_TOP_TABS, UNSELECTED_BOTTOM_TABS}) {
			Arrays.fill(tabs, tabs[0]);
		}
		for (Identifier[] tabs : new Identifier[][] {SELECTED_TOP_TABS, SELECTED_BOTTOM_TABS}) {
			Arrays.fill(tabs, 2, tabs.length - 1, tabs[1]);
		}
	}

	/** The creative inventory tab mirrors every slot of the survival inventory; keep ours off-screen there. */
	@ModifyArgs(method = "selectTab", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
	private void cassonade$hideGearSlots(Args args) {
		int index = args.get(1);
		if (index >= GearLayout.GEAR_START) {
			args.set(2, -2000);
			args.set(3, -2000);
		}
	}
}
