package com.cassonade.client.mixin;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;

import com.cassonade.client.ServerSelectionScreen;
import com.cassonade.client.WorldSelectionScreen;

/** The rows of the mod's server selection are drawn by it, the icon further in; vanilla still pings the server. */
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class OnlineServerEntryMixin {
	@Shadow
	@Final
	private JoinMultiplayerScreen screen;

	@Shadow
	@Final
	private ServerData serverData;

	@Shadow
	@Final
	private FaviconTexture icon;

	@Shadow
	private byte @Nullable [] lastIconBytes;

	@Shadow
	protected abstract boolean uploadServerIcon(byte @Nullable [] serverIconBytes);

	@Shadow
	public abstract void updateServerList();

	/** Vanilla's tests, from the row's content corner (2 pixels in): moved to where the mod draws the icon. */
	public boolean mouseOverRightHalf(int relX, int relY, int size) {
		int shift = cassonade$iconShift();
		// Alone in the list, nothing to move: the whole icon joins it, the arrow being in its middle
		int from = screen instanceof ServerSelectionScreen && screen.getServers().size() <= 1 ? 0 : size / 2;
		return relX >= shift + from && relX < shift + size && relY >= shift && relY < shift + size;
	}

	public boolean mouseOverTopLeftQuarter(int relX, int relY, int size) {
		int shift = cassonade$iconShift();
		return relX >= shift && relX < shift + size / 2 && relY >= shift && relY < shift + size / 2;
	}

	public boolean mouseOverBottomLeftQuarter(int relX, int relY, int size) {
		int shift = cassonade$iconShift();
		return relX >= shift && relX < shift + size / 2 && relY >= shift + size / 2 && relY < shift + size;
	}

	@Unique
	private int cassonade$iconShift() {
		return screen instanceof ServerSelectionScreen ? WorldSelectionScreen.ROW_ICON_INSET - 2 : 0;
	}

	/** At vanilla's first draw, past its ping: its icon's upload comes after the drawing skipped here, so it is done here. */
	@Inject(method = "extractContent", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), cancellable = true)
	private void cassonade$extractRow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
		if (screen instanceof ServerSelectionScreen servers) {
			byte[] iconBytes = serverData.getIconBytes();
			if (!Arrays.equals(iconBytes, lastIconBytes)) {
				if (uploadServerIcon(iconBytes)) {
					lastIconBytes = iconBytes;
				} else {
					serverData.setIconBytes(null);
					updateServerList();
				}
			}
			servers.extractRow(graphics, (ServerSelectionList.OnlineServerEntry) (Object) this, icon.textureLocation(), hovered, mouseX, mouseY);
			ci.cancel();
		}
	}
}
