package com.cassonade.client.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;

import com.cassonade.client.InfoPanel;

/** Shows the server-synced time and remaining fuelled brews in an open brewing stand. */
@Mixin(BrewingStandScreen.class)
public abstract class BrewingStandScreenMixin extends AbstractContainerScreen<BrewingStandMenu> {
	private static final int SECONDS_PER_MINUTE = 60;

	private BrewingStandScreenMixin(BrewingStandMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void cassonade$brewingInfo(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		int ticks = menu.getBrewingTicks();
		int fuel = menu.getFuel();
		List<InfoPanel.Entry> entries = new ArrayList<>();
		if (ticks > 0) {
			int seconds = (ticks + 19) / 20;
			String time = seconds / SECONDS_PER_MINUTE + ":" + String.format("%02d", seconds % SECONDS_PER_MINUTE);
			entries.add(new InfoPanel.Entry(Component.translatable("gui.cassonade.info_panel.brewing"),
					Component.translatable("gui.cassonade.brewing.done_in", time)));
		}
		if (fuel > 0) {
			entries.add(new InfoPanel.Entry(Component.translatable("gui.cassonade.info_panel.fuel"),
					Component.translatable("gui.cassonade.brewing.brews_left", ticks > 0 ? fuel + 1 : fuel)));
		}
		if (entries.isEmpty()) {
			return;
		}
		InfoPanel.draw(graphics, font, entries, new InfoPanel.Window(leftPos, topPos, imageWidth, imageHeight, width, height), mouseX, mouseY);
	}
}
