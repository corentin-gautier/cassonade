package com.cassonade.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;

import com.cassonade.client.quest.QuestLog;
import com.cassonade.test.JournalOffer;

/** The journal's offer is declined on joining a world in the tests, except for the one testing it: it covers screenshots. */
@Mixin(QuestLog.class)
abstract class QuestLogMixin {
	@Inject(method = "open", at = @At("TAIL"))
	private static void cassonadeTest$decline(Minecraft minecraft, CallbackInfo ci) {
		if (!JournalOffer.shown && !QuestLog.hasChosen()) {
			QuestLog.setEnabled(false);
		}
	}
}
