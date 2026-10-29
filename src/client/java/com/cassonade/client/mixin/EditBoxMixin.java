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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.cassonade.Cassonade;
import com.cassonade.client.PaperEditBox;

/** Text fields that sit on paper, as the recipe book's search, are drawn on it rather than as a dark slot; the storage search is framed like a small window. */
@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget implements PaperEditBox {
	@Unique
	private static final WidgetSprites PAPER_SPRITES = new WidgetSprites(Cassonade.id("widget/paper_text_field"),
			Cassonade.id("widget/paper_text_field_highlighted"));
	/** A small window above the storage screens, around a sunk field (tools/search_field_texture.py). */
	@Unique
	private static final Identifier STORAGE_SPRITE = Cassonade.id("container/storage_search_field");
	/** The window's edge, the field's rim and two pixels of room before the text. */
	@Unique
	private static final int STORAGE_TEXT_INSET = 4 + 1 + 2;
	// The recipe book's inks (tools/gui_theme.py)
	@Unique
	private static final int INK = 0xFF3A2A18;
	@Unique
	private static final int FADED_INK = 0xFF76664C;

	@Unique
	private boolean cassonade$paper;
	@Unique
	private boolean cassonade$storage;

	private EditBoxMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Shadow
	public abstract void setTextColor(int textColor);

	@Shadow
	public abstract void setTextShadow(boolean textShadow);

	@Shadow
	public abstract void setHint(Component hint);

	@Shadow
	private int textX;

	@Shadow
	private void updateTextPosition() {
	}

	@Override
	public void cassonade$writeOnPaper(Component hint) {
		cassonade$paper = true;
		setTextColor(INK);
		setTextShadow(false);
		setHint(hint.copy().withColor(FADED_INK));
	}

	@Override
	public void cassonade$writeOnStorage(Component hint) {
		cassonade$storage = true;
		setTextColor(0xFFECD9B2);
		setTextShadow(false);
		setHint(hint.copy().withColor(0xFFAA9C8A));
		// Otherwise the cursor keeps vanilla's place until the text changes
		updateTextPosition();
	}

	/** The storage field is framed like a window: its text starts inside the sunk field, not at vanilla's 4 pixels. */
	@Inject(method = "updateTextPosition", at = @At("TAIL"))
	private void cassonade$storageTextPosition(CallbackInfo ci) {
		if (cassonade$storage) {
			textX += STORAGE_TEXT_INSET - 4;
		}
	}

	@Inject(method = "getInnerWidth", at = @At("RETURN"), cancellable = true)
	private void cassonade$storageInnerWidth(CallbackInfoReturnable<Integer> cir) {
		if (cassonade$storage) {
			cir.setReturnValue(cir.getReturnValueI() - 2 * (STORAGE_TEXT_INSET - 4));
		}
	}

	@ModifyArg(method = "extractWidgetRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private Identifier cassonade$sprite(Identifier sprite) {
		if (cassonade$paper) {
			return PAPER_SPRITES.get(isActive(), isFocused());
		}
		return cassonade$storage ? STORAGE_SPRITE : sprite;
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
