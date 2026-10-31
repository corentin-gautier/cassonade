package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

import com.cassonade.client.DialogPanel;

/** Vanilla's centered menu, with its title in the gold of the dialogs'. */
@Mixin(PauseScreen.class)
abstract class PauseScreenMixin {
	@WrapOperation(method = "init", at = @At(value = "NEW",
			target = "(IIIILnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/Font;)Lnet/minecraft/client/gui/components/StringWidget;"))
	private StringWidget cassonade$goldTitle(int x, int y, int width, int height, Component text, Font font, Operation<StringWidget> original) {
		return original.call(x, y, width, height, text.copy().withColor(DialogPanel.TITLE_COLOR), font);
	}
}
