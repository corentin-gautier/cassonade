package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import com.cassonade.client.ClosingScreen;
import com.cassonade.client.ServerSelectionScreen;
import com.cassonade.client.WorldSelectionScreen;
import com.cassonade.client.quest.AdvancementsListScreen;

/**
 * The advancements key and the pause menu's button open the list of advancements instead of the tree, and the
 * world and server selections open the mod's (WorldSelectionScreen, ServerSelectionScreen).
 * <p>
 * The toasts are drawn last, after the subtitles the game defers past the screens: the minimap and the clock
 * come with them (CassonadeClient), over an open inventory, and the toasts must stay in front of them.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@Shadow
	public abstract ToastManager toastManager();

	@Shadow
	private @Nullable Screen screen;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private @Nullable Screen cassonade$closing;

	/** The window closed is drawn a little longer, on its way out, over the game it gave back. */
	@Inject(method = "setScreen", at = @At("HEAD"))
	private void cassonade$startClosing(@Nullable Screen next, CallbackInfo ci) {
		if (screen != null) {
			cassonade$closing = next == null && minecraft.level != null && ((ClosingScreen) screen).startClosing() ? screen : null;
		}
	}

	@Inject(method = "setScreen", at = @At("TAIL"))
	private void cassonade$stopClosing(@Nullable Screen next, CallbackInfo ci) {
		if (screen != null) {
			cassonade$closing = null;
		}
	}

	@Inject(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Hud;extractDebugOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
	private void cassonade$drawClosing(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci,
			@Local GuiGraphicsExtractor graphics) {
		if (cassonade$closing == null || screen != null) {
			return;
		}
		if (((ClosingScreen) cassonade$closing).closingFinished()) {
			cassonade$closing = null;
		} else {
			cassonade$closing.extractRenderStateWithTooltipAndSubtitles(graphics, -1000, -1000, deltaTracker.getGameTimeDeltaTicks());
		}
	}

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
	private Screen cassonade$replaceScreens(Screen screen) {
		if (screen instanceof AdvancementsScreen tree) {
			return new AdvancementsListScreen(((AdvancementsScreenAccessor) tree).getLastScreen());
		}
		// Vanilla's exactly: not a mod's own subclass, nor this screen opened again
		if (screen != null && screen.getClass() == SelectWorldScreen.class) {
			return new WorldSelectionScreen(((SelectWorldScreenAccessor) screen).getLastScreen());
		}
		if (screen != null && screen.getClass() == JoinMultiplayerScreen.class) {
			return new ServerSelectionScreen(((JoinMultiplayerScreenAccessor) screen).getLastScreen());
		}
		return screen;
	}
}
