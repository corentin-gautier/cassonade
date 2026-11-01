package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.network.chat.Component;

/** Esc here goes back to the book, as "Cancel" does: closing outright would lose what was written. */
@Mixin(BookSignScreen.class)
public abstract class BookSignScreenMixin extends Screen {
	@Shadow
	@Final
	private BookEditScreen bookEditScreen;
	@Shadow
	private String titleValue;
	@Shadow
	private EditBox titleBox;

	protected BookSignScreenMixin(Component title) {
		super(title);
	}

	@Override
	public void onClose() {
		titleValue = titleBox.getValue();
		minecraft.gui.setScreen(bookEditScreen);
	}
}
