package com.cassonade.client.mixin;

import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Sign editing as in any text field: the mouse places the cursor and selects, the arrows cross lines and keep the
 * column, and with nothing selected copy and paste take the whole sign, to write a row of matching signs.
 */
@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen {
	@Shadow
	@Final
	protected SignBlockEntity sign;
	@Shadow
	@Final
	private String[] messages;
	@Shadow
	private int line;
	@Shadow
	@Final
	private TextFieldHelper signField;

	@Unique
	private boolean cassonade$selecting;

	protected AbstractSignEditScreenMixin(Component title) {
		super(title);
	}

	@Shadow
	protected abstract Vector3fc getSignTextScale();

	@Shadow
	protected abstract float getSignYOffset();

	@Shadow
	private void setMessage(String message) {
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		int row = cassonade$rowAt(event.y());
		double x = (event.x() - width / 2.0) / getSignTextScale().x();
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || row < 0 || Math.abs(x) > sign.getMaxTextLineWidth() / 2.0 + 2) {
			return false;
		}
		line = row;
		if (doubleClick) {
			signField.selectAll();
		} else {
			int position = cassonade$positionAt(event.x());
			signField.setSelectionRange(position, position);
			cassonade$selecting = true;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (cassonade$selecting) {
			signField.setCursorPos(cassonade$positionAt(event.x()), true);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		cassonade$selecting = false;
		return super.mouseReleased(event);
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void cassonade$editKeys(KeyEvent event, CallbackInfoReturnable<Boolean> info) {
		if (event.isUp() || event.isDown()) {
			int column = cassonade$cursorX();
			line = line + (event.isUp() ? -1 : 1) & 3;
			int position = cassonade$positionAtX(column);
			signField.setSelectionRange(position, position);
			info.setReturnValue(true);
		} else if (event.isLeft() && !event.hasShiftDown() && line > 0 && signField.getCursorPos() == 0 && !signField.isSelecting()) {
			line--;
			signField.setCursorToEnd();
			info.setReturnValue(true);
		} else if (event.isRight() && !event.hasShiftDown() && line < 3 && signField.getCursorPos() == messages[line].length() && !signField.isSelecting()) {
			line++;
			signField.setCursorToStart();
			info.setReturnValue(true);
		} else if (event.isCopy() && !signField.isSelecting()) {
			TextFieldHelper.setClipboardContents(minecraft, String.join("\n", messages));
			info.setReturnValue(true);
		} else if (event.isPaste() && TextFieldHelper.getClipboardContents(minecraft).contains("\n")) {
			cassonade$pasteSign(TextFieldHelper.getClipboardContents(minecraft).split("\n", -1));
			info.setReturnValue(true);
		}
	}

	/** Fills the whole sign, each line cut to what fits, as a sign copied from another wood may be wider. */
	@Unique
	private void cassonade$pasteSign(String[] pasted) {
		for (int i = messages.length - 1; i >= 0; i--) {
			line = i;
			setMessage(i < pasted.length ? font.plainSubstrByWidth(pasted[i], sign.getMaxTextLineWidth()) : "");
		}
		signField.setCursorToEnd();
	}

	@Unique
	private int cassonade$rowAt(double mouseY) {
		int lineHeight = sign.getTextLineHeight();
		double y = (mouseY - getSignYOffset()) / getSignTextScale().y() + 2.0 * lineHeight;
		int row = (int) Math.floor(y / lineHeight);
		return row >= 0 && row < messages.length ? row : -1;
	}

	@Unique
	private int cassonade$positionAt(double mouseX) {
		return cassonade$positionAtX((int) Math.round((mouseX - width / 2.0) / getSignTextScale().x()));
	}

	/** The cursor position closest to a distance from the sign's center, the lines being centered. */
	@Unique
	private int cassonade$positionAtX(int x) {
		String text = messages[line];
		int fromStart = x + font.width(text) / 2;
		int position = font.plainSubstrByWidth(text, fromStart).length();
		// Rounded to the closest side of the character clicked
		if (position < text.length() && fromStart - font.width(text.substring(0, position)) > font.width(text.substring(position, position + 1)) / 2) {
			position++;
		}
		return position;
	}

	@Unique
	private int cassonade$cursorX() {
		String text = messages[line];
		return font.width(text.substring(0, Math.min(signField.getCursorPos(), text.length()))) - font.width(text) / 2;
	}
}
