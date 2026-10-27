package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;

import com.cassonade.client.map.ChatCoordinates;
import com.cassonade.client.quest.QuestButtons;

/**
 * Rebuilding a screen's widgets clears them without the screen events Fabric sends when it opens:
 * the journal's and the advancements' buttons are added back.
 * <p>
 * In-game screens (inventories, books, the journals) only dim the world, where menus blur it: they
 * get both, so every interface sets itself apart from the world the same way.
 * <p>
 * Coordinates in the chat are links to the world map (ChatCoordinates).
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Shadow
	protected abstract void extractBlurredBackground(GuiGraphicsExtractor graphics);

	@Inject(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/Screen;extractTransparentBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
	private void cassonade$blurInGameUi(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		extractBlurredBackground(graphics);
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
