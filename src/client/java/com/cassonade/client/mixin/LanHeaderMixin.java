package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

import com.cassonade.client.ServerSelectionScreen;

/** The search on the local network wraps within the panel, narrower than vanilla's list. */
@Mixin(ServerSelectionList.LANHeader.class)
public abstract class LanHeaderMixin {
	@Inject(method = "extractContent", at = @At("HEAD"), cancellable = true)
	private void cassonade$extractRow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
		if (Minecraft.getInstance().gui.screen() instanceof ServerSelectionScreen servers) {
			servers.extractScanning(graphics, (ServerSelectionList.Entry) (Object) this);
			ci.cancel();
		}
	}
}
