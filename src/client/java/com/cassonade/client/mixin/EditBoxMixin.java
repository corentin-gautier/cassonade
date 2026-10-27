package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.cassonade.Cassonade;
import com.cassonade.client.PaperEditBox;

/** Text fields that sit on paper, as the recipe book's search, are drawn on it rather than as a dark slot. */
@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget implements PaperEditBox {
	@Unique
	private static final WidgetSprites PAPER_SPRITES = new WidgetSprites(Cassonade.id("widget/paper_text_field"),
			Cassonade.id("widget/paper_text_field_highlighted"));
	// The recipe book's inks (tools/gui_theme.py)
	@Unique
	private static final int INK = 0xFF3A2A18;
	@Unique
	private static final int FADED_INK = 0xFF76664C;

	@Unique
	private boolean cassonade$paper;

	private EditBoxMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Shadow
	public abstract void setTextColor(int textColor);

	@Shadow
	public abstract void setTextShadow(boolean textShadow);

	@Shadow
	public abstract void setHint(Component hint);

	@Override
	public void cassonade$writeOnPaper(Component hint) {
		cassonade$paper = true;
		setTextColor(INK);
		setTextShadow(false);
		setHint(hint.copy().withColor(FADED_INK));
	}

	@ModifyArg(method = "extractWidgetRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private Identifier cassonade$paperSprite(Identifier sprite) {
		return cassonade$paper ? PAPER_SPRITES.get(isActive(), isFocused()) : sprite;
	}

	/** The hint is drawn with a shadow, whatever the text's: none on paper. */
	@WrapOperation(method = "extractWidgetRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private void cassonade$paperHint(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color, Operation<Void> original) {
		if (cassonade$paper) {
			graphics.text(font, text, x, y, color, false);
		} else {
			original.call(graphics, font, text, x, y, color);
		}
	}
}
