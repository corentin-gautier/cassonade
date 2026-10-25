package com.cassonade.client.mixin;

import java.util.List;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.Mth;

import com.cassonade.client.ChatLayout;
import com.cassonade.client.map.FramedPanel;

/**
 * The chat's messages are written inside a frame and its padding, above the open chat's input field
 * (ChatLayout), and while it is open they sit on a framed panel. Their colors are in ChatColorsMixin.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	private List<GuiMessage.Line> trimmedMessages;

	@Shadow
	private int chatScrollbarPos;

	@Shadow
	public abstract int getLinesPerPage();

	@Shadow
	private double getScale() {
		throw new AssertionError();
	}

	@Shadow
	private int getLineHeight() {
		throw new AssertionError();
	}

	/** Unscaled, so the frame and the padding keep their size whatever the chat's scale. */
	@ModifyArg(method = "lambda$extractRenderState$0", at = @At(value = "INVOKE",
			target = "Lorg/joml/Matrix3x2f;translate(FF)Lorg/joml/Matrix3x2f;"), index = 0)
	private static float cassonade$textLeft(float x, @Local(argsOnly = true) float scale) {
		return ChatLayout.TEXT_LEFT / scale;
	}

	/**
	 * The messages end above the open chat's input field, in game too so they stay put when it opens:
	 * vanilla puts their bottom 40 pixels above the screen's. Drawing them and finding what is clicked
	 * both go through here.
	 */
	@ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int cassonade$aboveInput(int screenHeight) {
		return ChatLayout.linesBottom(minecraft, screenHeight) + 40;
	}

	/**
	 * The panel under the lines vanilla shows when the chat is open, all of them fully visible, and
	 * the restriction notice above them.
	 */
	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
			at = @At("HEAD"))
	private void cassonade$panel(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY,
			ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, CallbackInfo ci) {
		int lines = Math.min(trimmedMessages.size() - chatScrollbarPos, getLinesPerPage());
		if (!displayMode.foreground || (lines <= 0 && !displayMode.showRestrictedPrompt)) {
			return;
		}
		if (displayMode.showRestrictedPrompt) {
			lines++;
		}
		float scale = (float) getScale();
		// Rounded as vanilla does
		int chatBottom = Mth.floor(ChatLayout.linesBottom(minecraft, graphics.guiHeight()) / scale);
		int top = Mth.floor((chatBottom - lines * getLineHeight()) * scale) - ChatLayout.PADDING - FramedPanel.BORDER;
		FramedPanel.draw(graphics, ChatLayout.MARGIN, top, ChatLayout.right(minecraft) - ChatLayout.MARGIN,
				ChatLayout.historyBottom(graphics.guiHeight()) - top);
	}
}
