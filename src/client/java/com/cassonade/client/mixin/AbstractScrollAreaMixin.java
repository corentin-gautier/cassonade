package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.input.MouseButtonEvent;

import com.cassonade.client.ScrollSounds;
import com.cassonade.client.WorldSelectionScreen;

/**
 * Every list ticks as the wheel or its scrollbar moves it by a step ({@link ScrollSounds}), half a row in a list. The world
 * selection's scrollbar is drawn in the mod's style: a sunk groove and a wooden handle.
 */
@Mixin(AbstractScrollArea.class)
public abstract class AbstractScrollAreaMixin {
	@Shadow
	private boolean scrolling;

	@Shadow
	protected abstract boolean scrollable();

	@Shadow
	protected abstract int scrollBarX();

	@Shadow
	protected abstract int scrollerHeight();

	@Shadow
	public abstract int scrollBarY();

	@Shadow
	public abstract int scrollbarWidth();

	@Shadow
	protected abstract boolean isOverScrollbar(double x, double y);

	@Shadow
	public abstract double scrollAmount();

	@Shadow
	public abstract int maxScrollAmount();

	@Shadow
	protected abstract double scrollRate();

	@WrapMethod(method = "mouseScrolled")
	private boolean cassonade$wheelTicks(double x, double y, double scrollX, double scrollY, Operation<Boolean> original) {
		double before = scrollAmount();
		boolean handled = original.call(x, y, scrollX, scrollY);
		ScrollSounds.moved(before, scrollAmount(), scrollRate(), maxScrollAmount());
		return handled;
	}

	@WrapMethod(method = "mouseDragged")
	private boolean cassonade$dragTicks(MouseButtonEvent event, double dx, double dy, Operation<Boolean> original) {
		double before = scrollAmount();
		boolean handled = original.call(event, dx, dy);
		ScrollSounds.moved(before, scrollAmount(), scrollRate(), maxScrollAmount());
		return handled;
	}

	@Inject(method = "extractScrollbar", at = @At("HEAD"), cancellable = true)
	private void cassonade$worldsScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (!WorldSelectionScreen.styles(this)) {
			return;
		}
		ci.cancel();
		if (!scrollable()) {
			return;
		}
		AbstractScrollArea self = (AbstractScrollArea) (Object) this;
		boolean hovered = scrolling || isOverScrollbar(mouseX, mouseY);
		WorldSelectionScreen.extractScrollbar(graphics, scrollBarX(), self.getY(), self.getBottom(), scrollbarWidth(),
				scrollAmount() / maxScrollAmount(), scrollerHeight(), hovered);
		if (hovered) {
			graphics.requestCursor(scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
		}
	}
}
