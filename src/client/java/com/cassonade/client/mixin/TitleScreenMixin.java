package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The title screen's logo, at half its size, and buttons in a column on the left, centered in height, over the panorama
 * sinking towards the left into the color of the loading screens. The buttons stay vanilla's, only moved: those of the demo, the disabled multiplayer's tooltip and the Realms icons work
 * as ever.
 */
@Mixin(TitleScreen.class)
abstract class TitleScreenMixin extends Screen {
	@Unique
	private static final float LOGO_SCALE = 0.5f;
	/** The logo's 44 pixels and the 7 by which the edition's 14 hang below them, at that scale. */
	@Unique
	private static final int LOGO_HEIGHT = 26;
	@Unique
	private static final int LOGO_GAP = 30;
	@Unique
	private static final int MIN_MARGIN = 12;
	/** What vanilla keeps free along the bottom: the version and the credits. */
	@Unique
	private static final int FOOTER = 10;
	/** The color of the background that WorldLoading tiles behind the loading screens and the world selection. */
	@Unique
	private static final int SHADE = 0x191818;
	@Unique
	private static final int SHADE_ALPHA = 245;
	/** How far the shade reaches past the buttons, at least. */
	@Unique
	private static final int SHADE_PAST = 80;

	@Unique
	private int cassonade$left;
	@Unique
	private int cassonade$logoTop;
	@Unique
	private int cassonade$buttonsTop;

	private TitleScreenMixin(Component title) {
		super(title);
	}

	/** Every widget in vanilla's column of buttons sets the column's height, wherever the demo or another mod has put a row. */
	@Unique
	private void cassonade$layOut() {
		int vanillaLeft = width / 2 - 100;
		int vanillaRight = width / 2 + 100;
		int top = Integer.MAX_VALUE;
		int bottom = Integer.MIN_VALUE;
		for (var child : children()) {
			if (child instanceof AbstractWidget widget && !(widget instanceof PlainTextButton)
					&& widget.getX() < vanillaRight && widget.getX() + widget.getWidth() > vanillaLeft) {
				top = Math.min(top, widget.getY());
				bottom = Math.max(bottom, widget.getY() + widget.getHeight());
			}
		}
		cassonade$left = Math.max(MIN_MARGIN, width * 6 / 100);
		cassonade$logoTop = Math.max(2, (height - FOOTER - LOGO_HEIGHT - LOGO_GAP - (bottom - top)) / 2);
		cassonade$buttonsTop = cassonade$logoTop + LOGO_HEIGHT + LOGO_GAP;
		int icons = 0;
		for (var child : children()) {
			if (child instanceof AbstractWidget widget && !(widget instanceof PlainTextButton)) {
				widget.setY(widget.getY() + cassonade$buttonsTop - top);
				// Vanilla centers the icons in its column: they line up on the column's edge with the rest
				widget.setX(widget instanceof SpriteIconButton ? cassonade$left + icons++ * (widget.getWidth() + 4)
						: widget.getX() + cassonade$left - vanillaLeft);
			}
		}
	}

	/**
	 * The Realms icons place themselves on the Realms button from the size they are given: the right of a column
	 * centered in the width, a quarter of the height plus 48 to the first row. All the widgets exist by this last
	 * step of init, so the layout is done here, in one go.
	 */
	@WrapOperation(method = "init", at = @At(value = "INVOKE",
			target = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;init(II)V"))
	private void cassonade$layOutAndRealms(RealmsNotificationsScreen realms, int width, int height, Operation<Void> original) {
		cassonade$layOut();
		original.call(realms, (cassonade$left + 100) * 2, (cassonade$buttonsTop - 48) * 4);
	}

	/** The shade eases out to the right, a column at a time: the game's gradients only run downwards. */
	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/TitleScreen;extractPanorama(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"))
	private void cassonade$shade(TitleScreen screen, GuiGraphicsExtractor graphics, float a, Operation<Void> original) {
		original.call(screen, graphics, a);
		int reach = Math.max(width * 17 / 20, cassonade$left + 200 + SHADE_PAST);
		for (int x = 0; x < reach; x++) {
			int alpha = Math.round(SHADE_ALPHA * (1 - Mth.smoothstep((float) x / reach)));
			graphics.fill(x, 0, x + 1, height, alpha << 24 | SHADE);
		}
	}

	/** The logo centers itself in the width it is given: twice its own half puts it at the origin scaled here. */
	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/LogoRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V"))
	private void cassonade$logo(LogoRenderer logo, GuiGraphicsExtractor graphics, int width, float alpha, Operation<Void> original) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(cassonade$left, cassonade$logoTop);
		graphics.pose().scale(LOGO_SCALE, LOGO_SCALE);
		logo.extractRenderState(graphics, LogoRenderer.LOGO_WIDTH, alpha, 0);
		graphics.pose().popMatrix();
	}

	/** The splash text shrinks with the logo and keeps its place on its corner, which vanilla sets from its own logo's height. */
	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/SplashRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/client/gui/Font;F)V"))
	private void cassonade$splash(SplashRenderer splash, GuiGraphicsExtractor graphics, int width, Font font, float alpha, Operation<Void> original) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(cassonade$left, cassonade$logoTop);
		graphics.pose().scale(LOGO_SCALE, LOGO_SCALE);
		graphics.pose().translate(0, -LogoRenderer.DEFAULT_HEIGHT_OFFSET);
		original.call(splash, graphics, LogoRenderer.LOGO_WIDTH, font, alpha);
		graphics.pose().popMatrix();
	}
}
