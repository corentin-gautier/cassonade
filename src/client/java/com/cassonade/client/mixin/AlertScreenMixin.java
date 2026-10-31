package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.cassonade.client.DialogPanel;
import com.cassonade.client.map.FramedPanel;

/** The title, the message and the button, centered in height in a panel; vanilla puts them at fixed heights. */
@Mixin(AlertScreen.class)
abstract class AlertScreenMixin extends Screen {
	@Unique
	private static final int TITLE_GAP = 11;
	@Unique
	private static final int BUTTON_GAP = 12;
	@Unique
	private static final int VANILLA_TITLE_TOP = 70;

	@Shadow
	private MultiLineLabel message;

	@Unique
	private int cassonade$top;
	@Unique
	private int cassonade$contentHeight;

	private AlertScreenMixin(Component title) {
		super(title);
	}

	@ModifyArg(method = "init", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/MultiLineLabel;create(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;I)Lnet/minecraft/client/gui/components/MultiLineLabel;"))
	private int cassonade$narrowMessage(int width) {
		return Math.min(width, DialogPanel.MAX_TEXT_WIDTH);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void cassonade$layOut(CallbackInfo info) {
		int textHeight = message.getLineCount() * font.lineHeight;
		cassonade$contentHeight = font.lineHeight + TITLE_GAP + textHeight + BUTTON_GAP + 20;
		cassonade$top = Math.max(DialogPanel.PADDING, (height - cassonade$contentHeight) / 2);
		for (var child : children()) {
			if (child instanceof AbstractWidget widget) {
				widget.setY(cassonade$top + cassonade$contentHeight - 20);
			}
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int contentWidth = Math.max(150, Math.max(font.width(title), message.getWidth()));
		FramedPanel.draw(graphics, (width - contentWidth) / 2 - DialogPanel.PADDING, cassonade$top - DialogPanel.PADDING,
				contentWidth + 2 * DialogPanel.PADDING, cassonade$contentHeight + 2 * DialogPanel.PADDING);
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 3)
	private int cassonade$titleTop(int y) {
		return cassonade$top + y - VANILLA_TITLE_TOP;
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 4)
	private int cassonade$titleColor(int color) {
		return DialogPanel.TITLE_COLOR;
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/MultiLineLabel;visitLines(Lnet/minecraft/client/gui/TextAlignment;IIILnet/minecraft/client/gui/ActiveTextCollector;)I"), index = 2)
	private int cassonade$messageTop(int y) {
		return cassonade$top + font.lineHeight + TITLE_GAP;
	}
}
