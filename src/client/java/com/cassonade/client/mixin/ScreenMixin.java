package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import com.cassonade.client.quest.QuestButtons;

/**
 * Rebuilding a screen's widgets clears them without the screen events Fabric sends when it opens:
 * the journal's and the advancements' buttons are added back.
 * <p>
 * In-game screens (inventories, books, the journals) only dim the world, where menus blur it: they
 * get both, so every interface sets itself apart from the world the same way.
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

	@Inject(method = "rebuildWidgets", at = @At("TAIL"))
	private void cassonade$keepQuestButtons(CallbackInfo ci) {
		QuestButtons.addTo((Screen) (Object) this);
	}
}
