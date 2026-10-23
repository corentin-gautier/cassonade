package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.Screen;

import com.vanillasugar.client.quest.QuestButtons;

/**
 * Rebuilding a screen's widgets clears them without the screen events Fabric sends when it opens:
 * the journal's and the advancements' buttons are added back.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Inject(method = "rebuildWidgets", at = @At("TAIL"))
	private void vanillasugar$keepQuestButtons(CallbackInfo ci) {
		QuestButtons.addTo((Screen) (Object) this);
	}
}
