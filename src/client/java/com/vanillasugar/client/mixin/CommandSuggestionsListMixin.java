package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.Rect2i;

import com.vanillasugar.client.ChatLayout;

/**
 * In the chat, the commands' suggestions hang below its input field in a popup framed like the
 * tooltips, like their usage (CommandSuggestionsMixin).
 */
@Mixin(targets = "net.minecraft.client.gui.components.CommandSuggestions$SuggestionsList")
public abstract class CommandSuggestionsListMixin {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Rect2i;<init>(IIII)V"), index = 1)
	private int vanillasugar$belowInput(int y) {
		return Minecraft.getInstance().gui.screen() instanceof ChatScreen chat ? ChatLayout.popupRowsTop(chat.height) : y;
	}

	@Shadow
	@Final
	private Rect2i rect;

	/** In place of vanilla's dark rows (ChatScreenMixin clears them); the suggestions are written 1 pixel in. */
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void vanillasugar$popup(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (Minecraft.getInstance().gui.screen() instanceof ChatScreen) {
			ChatLayout.drawPopup(graphics, rect.getX() + 1, rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight());
		}
	}
}
