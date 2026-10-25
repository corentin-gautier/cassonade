package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.util.ARGB;

/**
 * The chat's messages in game sit on the slots' dark brown (tools/gui_theme.py) rather than black, as
 * see-through as the text background option says. Open, see ChatOpenColorsMixin.
 */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess")
public abstract class ChatColorsMixin {
	/** The theme's FILL, the inside of slots. */
	@Unique
	private static final int BACKGROUND = 0x2E1D1C;

	@ModifyArg(method = "fill", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), index = 4)
	private int cassonade$themeColor(int color) {
		return (color & 0xFFFFFF) == 0 ? ARGB.color(ARGB.alpha(color), BACKGROUND) : color;
	}
}
