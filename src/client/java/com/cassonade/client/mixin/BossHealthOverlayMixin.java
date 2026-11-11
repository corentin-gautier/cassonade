package com.cassonade.client.mixin;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;

import com.cassonade.client.BossBars;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/** The boss bars are drawn by the mod (BossBars); vanilla keeps their events, music, darkening and fog. */
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
	@Shadow
	@Final
	private Map<UUID, LerpingBossEvent> events;

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void cassonade$draw(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (!Settings.on(Setting.BOSS_BARS)) {
			return;
		}
		if (!events.isEmpty()) {
			graphics.nextStratum();
			BossBars.extract(graphics, events.values());
		}
		ci.cancel();
	}
}
