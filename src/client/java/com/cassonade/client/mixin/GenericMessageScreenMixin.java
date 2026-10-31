package com.cassonade.client.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;

import com.cassonade.client.DialogPanel;
import com.cassonade.client.loading.WorldLoading;
import com.cassonade.client.map.FramedPanel;

/** The steps before a singleplayer world loads and its saving on leaving, in its loading screen's frame; other messages in a panel on its plain background. */
@Mixin(GenericMessageScreen.class)
abstract class GenericMessageScreenMixin {
	private static final String CASSONADE$SAVING = "menu.savingLevel";
	private static final Set<String> CASSONADE$WORLD_STEPS = Set.of("selectWorld.data_read", "selectWorld.resource_load", CASSONADE$SAVING);

	@Shadow
	private FocusableTextWidget textWidget;

	private boolean cassonade$isWorldStep() {
		return ((Screen) (Object) this).getTitle().getContents() instanceof TranslatableContents contents
				&& CASSONADE$WORLD_STEPS.contains(contents.getKey());
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void cassonade$hideText(CallbackInfo info) {
		textWidget.visible = false;
	}

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void cassonade$extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo info) {
		Screen screen = (Screen) (Object) this;
		Font font = Minecraft.getInstance().font;
		if (cassonade$isWorldStep()) {
			if (screen.getTitle().getContents() instanceof TranslatableContents contents && contents.getKey().equals(CASSONADE$SAVING)) {
				WorldLoading.extractSaving(graphics, font, screen.width, screen.height, screen.getTitle(), WorldLoading.lastWorldName());
			} else {
				WorldLoading.extractWaiting(graphics, font, screen.width, screen.height, screen.getTitle(), true);
			}
		} else {
			// Vanilla draws the panorama here whatever the message
			WorldLoading.extractBackground(graphics, screen.width, screen.height);
			int textWidth = font.width(screen.getTitle());
			int panelWidth = textWidth + 2 * DialogPanel.PADDING;
			int panelHeight = font.lineHeight + 2 * DialogPanel.PADDING;
			FramedPanel.draw(graphics, (screen.width - panelWidth) / 2, (screen.height - panelHeight) / 2, panelWidth, panelHeight);
			graphics.centeredText(font, screen.getTitle(), screen.width / 2, (screen.height - font.lineHeight) / 2, 0xFFFFFFFF);
		}
		info.cancel();
	}
}
