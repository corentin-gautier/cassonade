package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.util.ARGB;

/**
 * The open chat's messages go without vanilla's black behind each line, the panel under them is
 * enough (ChatComponentMixin), and its scroll bar is wood (tools/gui_theme.py).
 */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess")
public abstract class ChatOpenColorsMixin {
	/** Vanilla's scroll bar, and its lit edge. */
	@Unique
	private static final int VANILLA_SCROLLER = 0x3333AA;
	@Unique
	private static final int VANILLA_SCROLLER_EDGE = 0xCCCCCC;
	/** The theme's WOOD_LIGHT and WIDGET_GLARE. */
	@Unique
	private static final int SCROLLER = 0x865E46;
	@Unique
	private static final int SCROLLER_EDGE = 0xEBD8C7;

	@ModifyArg(method = "fill", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), index = 4)
	private int cassonade$themeColor(int color) {
		return switch (color & 0xFFFFFF) {
			case 0 -> ARGB.transparent(color);
			case VANILLA_SCROLLER -> ARGB.color(ARGB.alpha(color), SCROLLER);
			case VANILLA_SCROLLER_EDGE -> ARGB.color(ARGB.alpha(color), SCROLLER_EDGE);
			default -> color;
		};
	}
}
