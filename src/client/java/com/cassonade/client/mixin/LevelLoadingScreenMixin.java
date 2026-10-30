package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.LevelLoadTracker;

import com.cassonade.client.loading.WorldLoading;

/** The map drawing itself while a world first loads (WorldLoading); portals and later changes of dimension stay vanilla. */
@Mixin(LevelLoadingScreen.class)
abstract class LevelLoadingScreenMixin {
	@Shadow
	private LevelLoadTracker loadTracker;
	@Shadow
	private float smoothedProgress;
	@Shadow
	private LevelLoadingScreen.Reason reason;

	private boolean cassonade$drawsMap() {
		return reason == LevelLoadingScreen.Reason.OTHER && WorldLoading.applies(loadTracker);
	}

	// After the narration, which stays
	@Inject(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/LevelLoadTracker;statusView()Lnet/minecraft/server/level/progress/ChunkLoadStatusView;"),
			cancellable = true)
	private void cassonade$extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo info) {
		if (cassonade$drawsMap()) {
			Screen screen = (Screen) (Object) this;
			WorldLoading.extract(graphics, Minecraft.getInstance().font, screen.width, screen.height, loadTracker, smoothedProgress);
			info.cancel();
		}
	}

	// The textures made for this screen, which the world map makes again if it needs them
	@Inject(method = "onClose", at = @At("HEAD"))
	private void cassonade$onClose(CallbackInfo info) {
		if (cassonade$drawsMap()) {
			WorldLoading.finished();
		}
	}

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void cassonade$extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo info) {
		if (cassonade$drawsMap()) {
			Screen screen = (Screen) (Object) this;
			WorldLoading.extractBackground(graphics, screen.width, screen.height);
			info.cancel();
		}
	}
}
