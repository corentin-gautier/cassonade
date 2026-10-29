package com.cassonade.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;

import com.cassonade.client.InfoPanel;
import com.cassonade.client.trader.TraderTimePanel;

@Mixin(MerchantScreen.class)
public abstract class WanderingTraderMerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
	private WanderingTraderMerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void cassonade$traderTime(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		Component line = TraderTimePanel.line(menu.containerId);
		if (line != null) {
			InfoPanel.draw(graphics, font, List.of(new InfoPanel.Entry(
					Component.translatable("gui.cassonade.info_panel.departure"), line)), new InfoPanel.Window(leftPos, topPos, imageWidth, imageHeight, width, height), mouseX, mouseY);
		}
	}
}
