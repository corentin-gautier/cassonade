package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.cassonade.client.ChatLayout;
import com.cassonade.client.map.FramedPanel;

/**
 * The chat's input is written on a framed panel right below its history, as wide (ChatLayout), and
 * the commands' suggestions and usage are listed below it in popups framed like the tooltips
 * (CommandSuggestionsMixin).
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
	/** None: the popups behind the suggestions and the usage are enough. */
	@Unique
	private static final int SUGGESTIONS_BACKGROUND = 0;

	@Shadow
	protected EditBox input;

	private ChatScreenMixin(Component title) {
		super(title);
	}

	/** EditBox writes its text at its top when it has no border. */
	@Inject(method = "init", at = @At("TAIL"))
	private void cassonade$inPanel(CallbackInfo ci) {
		input.setX(ChatLayout.TEXT_LEFT);
		input.setY(ChatLayout.inputTextTop(height));
		input.setWidth(ChatLayout.right(minecraft) - FramedPanel.BORDER - ChatLayout.PADDING - ChatLayout.TEXT_LEFT);
	}

	@ModifyArg(method = "init", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/CommandSuggestions;<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/client/gui/Font;ZZIIZI)V"),
			index = 9)
	private int cassonade$suggestionsBackground(int color) {
		return SUGGESTIONS_BACKGROUND;
	}

	/** As many suggestions as fit below the input field, 12 pixels each; the others scroll. */
	@ModifyArg(method = "init", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/CommandSuggestions;<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/client/gui/Font;ZZIIZI)V"),
			index = 7)
	private int cassonade$suggestionsBelowInput(int lineLimit) {
		return Math.min(lineLimit, ChatLayout.popupRows(height));
	}

	/** In place of vanilla's dark bar across the screen. */
	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V", ordinal = 0))
	private void cassonade$inputPanel(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color, Operation<Void> original) {
		FramedPanel.draw(graphics, ChatLayout.MARGIN, ChatLayout.inputTop(height), ChatLayout.right(minecraft) - ChatLayout.MARGIN,
				ChatLayout.INPUT_HEIGHT);
	}

	/** The suggestions and the usage cover the hearts and the hotbar: in front of them. */
	@Inject(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/CommandSuggestions;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
	private void cassonade$suggestionsInFront(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		graphics.nextStratum();
	}
}
