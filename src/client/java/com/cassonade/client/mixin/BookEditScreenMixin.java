package com.cassonade.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Inserts a page before the one open, or deletes it, on the client's copy: as for any edit, leaving without "Done" discards it. */
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
	@Unique
	private static final int MAX_PAGES = 100;

	@Shadow
	private int currentPage;
	@Shadow
	@Final
	private List<String> pages;
	@Shadow
	private MultiLineEditBox page;

	@Unique
	private Button cassonade$insertPage;
	@Unique
	private Button cassonade$deletePage;

	protected BookEditScreenMixin(Component title) {
		super(title);
	}

	@Shadow
	private void updatePageContent() {
	}

	@Shadow
	private void updateButtonVisibility() {
	}

	@Shadow
	private int menuControlsTop() {
		return 0;
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void cassonade$addPageButtons(CallbackInfo info) {
		cassonade$insertPage = addRenderableWidget(Button.builder(Component.literal("+"), button -> {
			pages.add(currentPage, "");
			cassonade$pagesChanged();
		}).bounds(width / 2 - 124, menuControlsTop(), 20, 20)
				.tooltip(Tooltip.create(Component.translatable("gui.cassonade.book.insert_page"))).build());
		cassonade$deletePage = addRenderableWidget(Button.builder(Component.literal("-"), button -> {
			pages.remove(currentPage);
			if (pages.isEmpty()) {
				pages.add("");
			}
			currentPage = Math.min(currentPage, pages.size() - 1);
			cassonade$pagesChanged();
		}).bounds(width / 2 + 104, menuControlsTop(), 20, 20)
				.tooltip(Tooltip.create(Component.translatable("gui.cassonade.book.delete_page"))).build());
		cassonade$insertPage.active = pages.size() < MAX_PAGES;
	}

	@Unique
	private void cassonade$pagesChanged() {
		updatePageContent();
		updateButtonVisibility();
		cassonade$insertPage.active = pages.size() < MAX_PAGES;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean handled = super.mouseClicked(event, doubleClick);
		// Back to writing: the click leaves the focus on the page button, which would swallow what is typed next
		if (getFocused() == cassonade$insertPage || getFocused() == cassonade$deletePage) {
			setFocused(page);
		}
		return handled;
	}
}
