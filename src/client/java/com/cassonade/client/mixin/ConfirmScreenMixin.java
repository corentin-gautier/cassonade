package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.cassonade.client.DialogPanel;

/** Vanilla's layout, in a panel. */
@Mixin(ConfirmScreen.class)
abstract class ConfirmScreenMixin extends Screen {
	@Shadow
	@Final
	protected LinearLayout layout;

	private ConfirmScreenMixin(Component title) {
		super(title);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		DialogPanel.around(graphics, layout, DialogPanel.PADDING);
	}

	@WrapOperation(method = "init", at = @At(value = "NEW",
			target = "(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/Font;)Lnet/minecraft/client/gui/components/StringWidget;"))
	private StringWidget cassonade$goldTitle(Component text, Font font, Operation<StringWidget> original) {
		return original.call(text.copy().withColor(DialogPanel.TITLE_COLOR), font);
	}

	@ModifyArg(method = "addMessage", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/MultiLineTextWidget;setMaxWidth(I)Lnet/minecraft/client/gui/components/MultiLineTextWidget;"))
	private int cassonade$narrowMessage(int width) {
		return Math.min(width, DialogPanel.MAX_TEXT_WIDTH);
	}
}
