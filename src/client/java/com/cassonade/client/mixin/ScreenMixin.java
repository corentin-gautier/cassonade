package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Util;
import net.minecraft.network.chat.ClickEvent;

import com.cassonade.client.ClosingScreen;
import com.cassonade.client.OpenFade;
import com.cassonade.client.loading.WorldLoading;
import com.cassonade.client.map.ChatCoordinates;
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.client.quest.AdvancementsListScreen;
import com.cassonade.client.quest.QuestButtons;
import com.cassonade.client.quest.QuestJournalScreen;
import com.cassonade.client.quest.QuestOptInScreen;

/**
 * Rebuilding a screen's widgets clears them without the screen events Fabric sends when it opens:
 * the journal's and the advancements' buttons are added back.
 * <p>
 * In-game screens (inventories, books, the journals) only dim the world, where menus blur it: they
 * get both, so every interface sets itself apart from the world the same way.
 * <p>
 * Those windows also rise a few pixels into place as they open, and sink back as they close, so they do not
 * just pop in and out.
 * <p>
 * Coordinates in the chat are links to the world map (ChatCoordinates).
 */
@Mixin(Screen.class)
public abstract class ScreenMixin implements ClosingScreen {
	@Unique
	private static final long SLIDE_MILLIS = 150;
	@Unique
	private static final int SLIDE_PIXELS = 10;
	@Unique
	private static final float GROW = 0.04f;

	@Unique
	private long cassonade$openedAt;
	@Unique
	private long cassonade$closedAt;
	@Unique
	private boolean cassonade$sliding;
	@Unique
	private float cassonade$inverse;

	@Shadow
	protected Minecraft minecraft;

	@Shadow
	protected abstract void extractBlurredBackground(GuiGraphicsExtractor graphics);

	@Shadow
	public abstract boolean isInGameUi();

	/** Menus away from any world sit on the loading screens' background, as the world and server selections do; the title screen draws its own panorama. */
	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void cassonade$plainMenus(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (minecraft.level == null && !isInGameUi()) {
			Screen self = (Screen) (Object) this;
			WorldLoading.extractBackground(graphics, self.width, self.height);
			minecraft.gui.hud.extractDeferredSubtitles();
			ci.cancel();
		}
	}

	/** The blur and the dimming come and go with the window: the blur's radius grows, the dimming fades. */
	@WrapOperation(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/Screen;extractTransparentBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
	private void cassonade$blurInGameUi(Screen screen, GuiGraphicsExtractor graphics, Operation<Void> original) {
		extractBlurredBackground(graphics);
		float progress = cassonade$progress();
		OpenFade.setBlur(progress);
		OpenFade.set(progress);
		original.call(screen, graphics);
		OpenFade.set(1f);
	}

	@Override
	public boolean startClosing() {
		if (!cassonade$animated()) {
			return false;
		}
		cassonade$closedAt = Util.getMillis();
		OpenFade.closing(cassonade$closedAt);
		return true;
	}

	@Override
	public boolean settled() {
		return !cassonade$animated() || cassonade$openedAt != 0 && Util.getMillis() - cassonade$openedAt >= SLIDE_MILLIS;
	}

	@Override
	public boolean closingFinished() {
		return Util.getMillis() - cassonade$closedAt >= SLIDE_MILLIS;
	}

	@Unique
	private boolean cassonade$animated() {
		Screen self = (Screen) (Object) this;
		return self instanceof AbstractContainerScreen || self instanceof WorldMapScreen
				|| self instanceof QuestJournalScreen || self instanceof AdvancementsListScreen
				|| self instanceof QuestOptInScreen;
	}

	/** The window moves with its panel, drawn by extractBackground: the blur and the dimming stay put. */
	@Inject(method = "extractBackground", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/gui/screens/Screen;extractTransparentBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
	private void cassonade$slideInPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		cassonade$startSlide(graphics);
	}

	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
	private void cassonade$slideIn(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		cassonade$startSlide(graphics);
	}

	/** The minimap and the clock are drawn with the subtitles the game defers there: they stay where they are. */
	@Inject(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Hud;extractDeferredSubtitles()V"))
	private void cassonade$hudStays(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (cassonade$sliding) {
			graphics.pose().popMatrix();
			OpenFade.set(1f);
		}
	}

	@Inject(method = "extractBackground", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/gui/Hud;extractDeferredSubtitles()V"))
	private void cassonade$hudStaysEnd(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (cassonade$sliding) {
			graphics.pose().pushMatrix();
			cassonade$transform(graphics);
			OpenFade.set(1f - cassonade$inverse);
		}
	}

	/** Grows from the middle of the screen while rising, so the window settles into place. */
	@Unique
	private void cassonade$transform(GuiGraphicsExtractor graphics) {
		Screen self = (Screen) (Object) this;
		float scale = 1f - GROW * cassonade$inverse;
		graphics.pose().translate(self.width / 2f, self.height / 2f + Math.round(SLIDE_PIXELS * cassonade$inverse));
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(-self.width / 2f, -self.height / 2f);
	}

	/** How far the window is shown, from 0 to 1: opening or closing; 1 for the screens that do not animate. */
	@Unique
	private float cassonade$progress() {
		if (!cassonade$animated()) {
			return 1f;
		}
		long now = Util.getMillis();
		if (cassonade$closedAt != 0) {
			return 1f - Math.min(1f, (now - cassonade$closedAt) / (float) SLIDE_MILLIS);
		}
		if (cassonade$openedAt == 0) {
			cassonade$openedAt = now;
		}
		return Math.min(1f, (now - cassonade$openedAt) / (float) SLIDE_MILLIS);
	}

	@Unique
	private void cassonade$startSlide(GuiGraphicsExtractor graphics) {
		if (cassonade$sliding) {
			return;
		}
		float inverse = 1f - cassonade$progress();
		if (inverse > 0f) {
			cassonade$sliding = true;
			graphics.pose().pushMatrix();
			cassonade$inverse = inverse * inverse * inverse;
			cassonade$transform(graphics);
			OpenFade.set(1f - cassonade$inverse);
		}
	}

	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
	private void cassonade$slideEnd(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (cassonade$sliding) {
			graphics.pose().popMatrix();
			OpenFade.set(1f);
			cassonade$sliding = false;
		}
	}

	/** A click on coordinates in the chat opens the world map there; kept on the client, unlike other custom clicks. */
	@Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
	private static void cassonade$coordinatesClicked(ClickEvent event, Minecraft minecraft, @Nullable Screen activeScreen, CallbackInfo ci) {
		if (event instanceof ClickEvent.Custom custom && custom.id().equals(ChatCoordinates.CLICK)) {
			ChatCoordinates.clicked(custom.payload());
			ci.cancel();
		}
	}

	@Inject(method = "rebuildWidgets", at = @At("TAIL"))
	private void cassonade$keepQuestButtons(CallbackInfo ci) {
		QuestButtons.addTo((Screen) (Object) this);
	}
}
