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
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.cassonade.client.DialogPanel;
import com.cassonade.client.map.FramedPanel;

/** Vanilla's arrangement moved as one block to the middle of the screen, in a panel. */
@Mixin(BackupConfirmScreen.class)
abstract class BackupConfirmScreenMixin extends Screen {
	@Unique
	private static final int VANILLA_TOP = 50;
	/** The width of vanilla's two buttons side by side. */
	@Unique
	private static final int BUTTONS_WIDTH = 310;

	@Shadow
	private MultiLineLabel message;

	@Unique
	private int cassonade$shift;
	@Unique
	private int cassonade$contentHeight;

	private BackupConfirmScreenMixin(Component title) {
		super(title);
	}

	@ModifyArg(method = "init", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/MultiLineLabel;create(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;I)Lnet/minecraft/client/gui/components/MultiLineLabel;"))
	private int cassonade$narrowMessage(int width) {
		return Math.min(width, DialogPanel.MAX_TEXT_WIDTH);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void cassonade$layOut(CallbackInfo info) {
		int bottom = 0;
		for (var child : children()) {
			if (child instanceof AbstractWidget widget) {
				bottom = Math.max(bottom, widget.getY() + widget.getHeight());
			}
		}
		cassonade$contentHeight = bottom - VANILLA_TOP;
		cassonade$shift = Math.max(DialogPanel.PADDING, (height - cassonade$contentHeight) / 2) - VANILLA_TOP;
		for (var child : children()) {
			if (child instanceof AbstractWidget widget) {
				widget.setY(widget.getY() + cassonade$shift);
			}
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int contentWidth = Math.max(BUTTONS_WIDTH, message.getWidth());
		FramedPanel.draw(graphics, (width - contentWidth) / 2 - DialogPanel.PADDING, VANILLA_TOP + cassonade$shift - DialogPanel.PADDING,
				contentWidth + 2 * DialogPanel.PADDING, cassonade$contentHeight + 2 * DialogPanel.PADDING);
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 3)
	private int cassonade$titleTop(int y) {
		return y + cassonade$shift;
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 4)
	private int cassonade$titleColor(int color) {
		return DialogPanel.TITLE_COLOR;
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/MultiLineLabel;visitLines(Lnet/minecraft/client/gui/TextAlignment;IIILnet/minecraft/client/gui/ActiveTextCollector;)I"), index = 2)
	private int cassonade$messageTop(int y) {
		return y + cassonade$shift;
	}
}
