package com.cassonade.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.cassonade.Cassonade;
import com.cassonade.client.DialogPanel;
import com.cassonade.client.map.DeathMarker;
import com.cassonade.client.map.FramedPanel;

/**
 * Vanilla's red veil over the world, with a skull above a panel gathering the title, the cause, the score and the buttons.
 * The respawn and title screen buttons are clickable at once instead of after one second.
 */
@Mixin(DeathScreen.class)
abstract class DeathScreenMixin extends Screen {
	@Unique
	private static final int TITLE_COLOR = 0xFFE8604F;
	@Unique
	private static final int BUTTON_WIDTH = 200;
	@Unique
	private static final int CAUSE_TOP = 28;
	@Unique
	private static final int SCORE_TOP = 42;
	@Unique
	private static final int BUTTONS_TOP = 65;
	@Unique
	private static final int HEIGHT = 109;
	@Unique
	private static final Identifier ICON = Cassonade.id("loading/death");
	/** The same size as the saving screen's disk. */
	@Unique
	private static final int ICON_SIZE = 64;
	@Unique
	private static final int ICON_GAP = 10;

	@Shadow
	@Final
	private @Nullable Component causeOfDeath;
	@Shadow
	@Final
	private Component deathScore;

	@Unique
	private int cassonade$top;
	@Unique
	private boolean cassonade$icon;

	private DeathScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void cassonade$showNewDeathMarker(CallbackInfo ci) {
		DeathMarker.onDeath();
	}

	@ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/DeathScreen;setButtonsActive(Z)V"))
	private boolean cassonade$noButtonDelay(boolean isActive) {
		return true;
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void cassonade$layOut(CallbackInfo ci) {
		int panelHeight = HEIGHT + 2 * DialogPanel.PADDING;
		int iconHeight = ICON_SIZE + ICON_GAP;
		// A very small window keeps the panel, in the middle, and loses the icon
		cassonade$icon = iconHeight + panelHeight <= height;
		cassonade$top = (height - panelHeight - (cassonade$icon ? iconHeight : 0)) / 2 + (cassonade$icon ? iconHeight : 0) + DialogPanel.PADDING;
		int y = cassonade$top + BUTTONS_TOP;
		for (var child : children()) {
			if (child instanceof AbstractWidget widget) {
				widget.setY(y);
				y += widget.getHeight() + 4;
			}
		}
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void cassonade$panel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		int contentWidth = Math.max(BUTTON_WIDTH, Math.max(font.width(title) * 2, font.width(deathScore)));
		if (causeOfDeath != null) {
			contentWidth = Math.max(contentWidth, font.width(causeOfDeath));
		}
		FramedPanel.draw(graphics, (width - contentWidth) / 2 - DialogPanel.PADDING, cassonade$top - DialogPanel.PADDING,
				contentWidth + 2 * DialogPanel.PADDING, HEIGHT + 2 * DialogPanel.PADDING);
		if (cassonade$icon) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON, (width - ICON_SIZE) / 2,
					cassonade$top - DialogPanel.PADDING - ICON_GAP - ICON_SIZE, ICON_SIZE, ICON_SIZE);
		}
	}

	// The title is drawn at twice the scale, its position included
	@ModifyArg(method = "visitText", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V"), index = 2)
	private int cassonade$titleTop(int y) {
		return cassonade$top / 2;
	}

	@ModifyArg(method = "visitText", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V"), index = 3)
	private Component cassonade$titleColor(Component text) {
		return text.copy().withColor(TITLE_COLOR);
	}

	@ModifyArg(method = "visitText", at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V"), index = 2)
	private int cassonade$causeTop(int y) {
		return cassonade$top + CAUSE_TOP;
	}

	@ModifyArg(method = "visitText", at = @At(value = "INVOKE", ordinal = 2,
			target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V"), index = 2)
	private int cassonade$scoreTop(int y) {
		return cassonade$top + SCORE_TOP;
	}
}
