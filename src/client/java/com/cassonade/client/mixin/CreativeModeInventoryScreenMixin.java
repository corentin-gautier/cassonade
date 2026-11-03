package com.cassonade.client.mixin;

import java.util.Arrays;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import com.cassonade.client.DescriptionText;
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

	/** The tabs an item is found in, under its name, in the tooltips' small and dim secondary text rather than blue. */
	@Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"))
	private void cassonade$dimTabs(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
		List<Component> lines = cir.getReturnValue();
		TextColor blue = TextColor.fromLegacyFormat(ChatFormatting.BLUE);
		for (int i = 1; i < lines.size(); i++) {
			if (blue.equals(lines.get(i).getStyle().getColor())) {
				lines.set(i, DescriptionText.of(lines.get(i)));
			}
		}
	}
}
