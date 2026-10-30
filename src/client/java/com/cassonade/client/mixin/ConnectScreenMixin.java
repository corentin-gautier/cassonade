package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.cassonade.client.loading.WorldLoading;

/** Connecting to a server, in the frame of the loading screen that follows; the cancel button takes the tip's place. */
@Mixin(ConnectScreen.class)
abstract class ConnectScreenMixin extends Screen {
	@Shadow
	private Component status;

	private ConnectScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void cassonade$init(CallbackInfo info) {
		WorldLoading.startJoining();
		int buttonWidth = WorldLoading.frameWidth(font, height);
		for (var child : children()) {
			if (child instanceof AbstractWidget button) {
				button.setWidth(buttonWidth);
				button.setPosition((width - buttonWidth) / 2, WorldLoading.tipTop(font, height));
			}
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		WorldLoading.extractWaiting(graphics, font, width, height, status, false);
	}

	@Redirect(method = "startConnecting", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;disconnectWithProgressScreen(Z)V"))
	private static void cassonade$closeInFrame(Minecraft minecraft, boolean stopSound) {
		WorldLoading.between(false, null, () -> minecraft.disconnectWithProgressScreen(stopSound));
	}

	// The status is written over the bar, by the background
	@Redirect(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private void cassonade$skipStatus(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
	}
}
