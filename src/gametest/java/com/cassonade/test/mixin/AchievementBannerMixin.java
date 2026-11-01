package com.cassonade.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.advancements.AdvancementHolder;

import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.test.TestNotifications;

/** No advancement banner in the tests, except for those testing it: it costs time and covers screenshots. */
@Mixin(AchievementBanner.class)
abstract class AchievementBannerMixin {
	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private static void cassonadeTest$mute(AdvancementHolder advancement, CallbackInfo ci) {
		if (!TestNotifications.shown) {
			ci.cancel();
		}
	}
}
