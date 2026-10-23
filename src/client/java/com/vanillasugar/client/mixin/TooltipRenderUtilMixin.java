package com.vanillasugar.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Tooltips get more room around their contents: 6 pixels instead of vanilla's 3. */
@Mixin(TooltipRenderUtil.class)
public abstract class TooltipRenderUtilMixin {
	@Unique
	private static final int PADDING = 6;
	/** Vanilla's sprites reach this far out of the padding, see TooltipRenderUtil. */
	@Unique
	private static final int SPRITE_MARGIN = 9;

	@Shadow
	private static Identifier getBackgroundSprite(@Nullable Identifier style) {
		throw new AssertionError();
	}

	@Shadow
	private static Identifier getFrameSprite(@Nullable Identifier style) {
		throw new AssertionError();
	}

	@Inject(method = "extractTooltipBackground", at = @At("HEAD"), cancellable = true)
	private static void vanillasugar$widerPadding(GuiGraphicsExtractor graphics, int x, int y, int w, int h, @Nullable Identifier style, CallbackInfo ci) {
		int x0 = x - PADDING - SPRITE_MARGIN;
		int y0 = y - PADDING - SPRITE_MARGIN;
		int width = w + 2 * (PADDING + SPRITE_MARGIN);
		int height = h + 2 * (PADDING + SPRITE_MARGIN);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getBackgroundSprite(style), x0, y0, width, height);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getFrameSprite(style), x0, y0, width, height);
		ci.cancel();
	}
}
