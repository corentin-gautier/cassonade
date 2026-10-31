package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import com.mojang.blaze3d.platform.FramerateLimitTracker;

import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/**
 * A window in the background draws at the rate vanilla keeps for a player away from the keyboard: it is still
 * watched, but no longer played. Vanilla's option to limit only a minimized window turns this off as well.
 */
@Mixin(FramerateLimitTracker.class)
public abstract class FramerateLimitTrackerMixin {
	private static final int cassonade$UNFOCUSED_LIMIT = 30;

	@Shadow
	@Final
	private Options options;

	@Shadow
	@Final
	private Minecraft minecraft;

	@ModifyReturnValue(method = "getFramerateLimit", at = @At("RETURN"))
	private int cassonade$limitUnfocused(int limit) {
		return options.inactivityFpsLimit().get() == InactivityFpsLimit.AFK && !minecraft.getWindow().isFocused()
				? Math.min(limit, cassonade$UNFOCUSED_LIMIT) : limit;
	}
}
