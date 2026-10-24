package com.vanillasugar.client.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import com.vanillasugar.client.ChatLayout;
import com.vanillasugar.client.map.SmallText;

/**
 * In the chat, the commands' usage and errors hang below its input field (ChatLayout) in small text,
 * in a popup framed like the tooltips, as their suggestions do (CommandSuggestionsListMixin), clear of the history.
 */
@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
	@Shadow
	@Final
	private Screen screen;

	@Shadow
	@Final
	private List<FormattedCharSequence> commandUsage;

	@Shadow
	private int commandUsagePosition;

	@Shadow
	private int commandUsageWidth;

	@Shadow
	@Final
	private Font font;

	@Shadow
	@Final
	private EditBox input;

	/**
	 * Vanilla's place, under the cursor but kept left of the field's right edge with the text's full
	 * size: here with its small size, and never left of the field, where a message wider than the field
	 * would go.
	 */
	@Unique
	private int vanillasugar$usageLeft() {
		int right = input.getX() + input.getInnerWidth() - Mth.ceil(commandUsageWidth * SmallText.scale());
		return Math.max(input.getX(), Math.min(commandUsagePosition, right));
	}

	/** Vanilla stacks them upwards, over the history, 12 pixels apart; here downwards, a small line apart. */
	@ModifyVariable(method = "extractUsage", at = @At("STORE"), ordinal = 1)
	private int vanillasugar$belowInput(int lineY, @Local(ordinal = 0) int line) {
		return screen instanceof ChatScreen ? ChatLayout.popupRowsTop(screen.height) + SmallText.lineHeight(font) * line : lineY;
	}

	/** One popup for all the lines, in place of vanilla's dark band behind each (ChatScreenMixin clears it). */
	@Inject(method = "extractUsage", at = @At("HEAD"))
	private void vanillasugar$usagePopup(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (screen instanceof ChatScreen && !commandUsage.isEmpty()) {
			int top = ChatLayout.popupRowsTop(screen.height);
			// Vanilla writes them 2 pixels below their row's top
			int left = vanillasugar$usageLeft();
			ChatLayout.drawPopup(graphics, left, top, left + Mth.ceil(commandUsageWidth * SmallText.scale()),
					top + 2 + SmallText.lineHeight(font) * commandUsage.size());
		}
	}

	/** In small text, so a long error takes less room over the hearts and the hotbar. */
	@WrapOperation(method = "extractUsage", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"))
	private void vanillasugar$smallText(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int x, int y, int color, Operation<Void> original) {
		if (!(screen instanceof ChatScreen)) {
			original.call(graphics, font, text, x, y, color);
			return;
		}
		float scale = SmallText.scale();
		graphics.pose().pushMatrix();
		graphics.pose().translate(vanillasugar$usageLeft(), y);
		graphics.pose().scale(scale, scale);
		original.call(graphics, font, text, 0, 0, color);
		graphics.pose().popMatrix();
	}
}
