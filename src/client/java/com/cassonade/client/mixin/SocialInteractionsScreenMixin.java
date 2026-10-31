package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.resources.Identifier;

import com.cassonade.client.map.FramedPanel;

/** The mod's panel in place of vanilla's white frame, the only sprite here that is not shared with other screens. */
@Mixin(SocialInteractionsScreen.class)
abstract class SocialInteractionsScreenMixin {
	@WrapOperation(method = "extractBackground", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void cassonade$panel(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, Operation<Void> original) {
		FramedPanel.draw(graphics, x, y, width, height);
	}
}
