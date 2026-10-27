package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;

import com.cassonade.client.quest.AdvancementsListScreen;

/**
 * The advancements key and the pause menu's button open the list of advancements instead of the tree.
 * <p>
 * The toasts are drawn last, after the subtitles the game defers past the screens: the minimap and the clock
 * come with them (CassonadeClient), over an open inventory, and the toasts must stay in front of them.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@Shadow
	public abstract ToastManager toastManager();

	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
	private void cassonade$toastsLater(ToastManager toasts, GuiGraphicsExtractor graphics, Operation<Void> original) {
	}

	@Inject(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Hud;extractDeferredSubtitles()V", shift = At.Shift.AFTER))
	private void cassonade$toastsLast(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci,
			@Local GuiGraphicsExtractor graphics) {
		if (resourcesLoaded) {
			toastManager().extractRenderState(graphics);
		}
	}

	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen cassonade$listAdvancements(Screen screen) {
		if (screen instanceof AdvancementsScreen tree) {
			return new AdvancementsListScreen(((AdvancementsScreenAccessor) tree).getLastScreen());
		}
		return screen;
	}
}
