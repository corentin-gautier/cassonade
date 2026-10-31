package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

import com.cassonade.client.ServerSelectionScreen;

/** The worlds found on the local network, drawn as cards like the servers. */
@Mixin(ServerSelectionList.NetworkServerEntry.class)
public abstract class NetworkServerEntryMixin {
	@Shadow
	@Final
	private JoinMultiplayerScreen screen;

	@Inject(method = "extractContent", at = @At("HEAD"), cancellable = true)
	private void cassonade$extractRow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
		if (screen instanceof ServerSelectionScreen servers) {
			servers.extractRow(graphics, (ServerSelectionList.NetworkServerEntry) (Object) this, hovered);
			ci.cancel();
		}
	}
}
