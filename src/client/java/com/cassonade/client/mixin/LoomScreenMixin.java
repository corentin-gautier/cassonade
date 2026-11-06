package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.MouseButtonEvent;

import com.cassonade.client.ScrollSounds;

/** A tick for each row of patterns the wheel or the scrollbar moves the list by ({@link ScrollSounds}). */
@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin {
	@Shadow
	private int startRow;

	@WrapMethod(method = "mouseScrolled")
	private boolean cassonade$wheelTicks(double x, double y, double scrollX, double scrollY, Operation<Boolean> original) {
		int before = startRow;
		boolean handled = original.call(x, y, scrollX, scrollY);
		ScrollSounds.moved(before, startRow);
		return handled;
	}

	@WrapMethod(method = "mouseDragged")
	private boolean cassonade$dragTicks(MouseButtonEvent event, double dx, double dy, Operation<Boolean> original) {
		int before = startRow;
		boolean handled = original.call(event, dx, dy);
		ScrollSounds.moved(before, startRow);
		return handled;
	}
}
