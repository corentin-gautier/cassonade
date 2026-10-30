package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.cassonade.client.loading.WorldLoading;

/** The frame the game shows this screen for between the steps of opening a world (WorldLoading.between); otherwise vanilla. */
@Mixin(ProgressScreen.class)
abstract class ProgressScreenMixin extends Screen {
	private ProgressScreenMixin(Component title) {
		super(title);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		if (WorldLoading.isBetweenSteps()) {
			WorldLoading.extractBetweenSteps(graphics, font, width, height);
		} else {
			super.extractBackground(graphics, mouseX, mouseY, a);
		}
	}
}
