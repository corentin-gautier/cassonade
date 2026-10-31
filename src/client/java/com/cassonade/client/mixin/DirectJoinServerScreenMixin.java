package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.cassonade.client.DialogPanel;
import com.cassonade.client.map.FramedPanel;

/** The form in a panel centered in height, with its two buttons side by side like every other dialog's. */
@Mixin(DirectJoinServerScreen.class)
abstract class DirectJoinServerScreenMixin extends Screen {
	@Unique
	private static final int WIDTH = 200;
	@Unique
	private static final int HEIGHT = 85;
	@Unique
	private static final int LABEL_TOP = 21;
	@Unique
	private static final int FIELD_TOP = 33;
	@Unique
	private static final int BUTTONS_TOP = 65;

	@Shadow
	private EditBox ipEdit;
	@Shadow
	private Button selectButton;

	@Unique
	private int cassonade$top;

	private DirectJoinServerScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void cassonade$layOut(CallbackInfo info) {
		cassonade$top = (height - HEIGHT) / 2;
		int left = (width - WIDTH) / 2;
		ipEdit.setY(cassonade$top + FIELD_TOP);
		int half = (WIDTH - 4) / 2;
		for (var child : children()) {
			if (child instanceof Button button) {
				button.setWidth(half);
				button.setPosition(button == selectButton ? left : left + half + 4, cassonade$top + BUTTONS_TOP);
			}
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		FramedPanel.draw(graphics, (width - WIDTH) / 2 - DialogPanel.PADDING, cassonade$top - DialogPanel.PADDING,
				WIDTH + 2 * DialogPanel.PADDING, HEIGHT + 2 * DialogPanel.PADDING);
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 3)
	private int cassonade$titleTop(int y) {
		return cassonade$top;
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 4)
	private int cassonade$titleColor(int color) {
		return DialogPanel.TITLE_COLOR;
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 3)
	private int cassonade$labelTop(int y) {
		return cassonade$top + LABEL_TOP;
	}

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 4)
	private int cassonade$labelColor(int color) {
		return DialogPanel.LABEL_COLOR;
	}
}
