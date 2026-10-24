package com.vanillasugar.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import com.vanillasugar.client.ChatLayout;

/**
 * In the chat, the commands' suggestions and usage rest on its input field (ChatLayout): vanilla
 * anchors them 15 pixels above the screen's bottom, where its field is.
 */
@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
	@Shadow
	@Final
	private Screen screen;

	@ModifyExpressionValue(method = {"showSuggestions", "extractUsage"}, at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/gui/screens/Screen;height:I"))
	private int vanillasugar$onInput(int height) {
		return screen instanceof ChatScreen ? ChatLayout.inputTop(height) + 15 : height;
	}
}
