package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.Minecraft;

import com.cassonade.client.loading.WorldLoading;

/** Keeps the frame the game closes a session in, before a singleplayer world loads or as it quits, in the loading screen's style. */
@Mixin(Minecraft.class)
abstract class MinecraftWorldLoadMixin {
	@Redirect(method = "doWorldLoad", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;disconnectWithProgressScreen()V"))
	private void cassonade$closeInFrame(Minecraft minecraft) {
		WorldLoading.between(true, null, minecraft::disconnectWithProgressScreen);
	}

	// The last frame after saving, when the game itself closes
	@Redirect(method = "exitWorldAndClose", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;disconnectWithProgressScreen()V"))
	private void cassonade$closeInFrameOnExit(Minecraft minecraft) {
		WorldLoading.between(false, minecraft.getSingleplayerServer() != null ? WorldLoading.lastWorldName() : null, minecraft::disconnectWithProgressScreen);
	}
}
