package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.gui.screens.social.PlayerEntry;

/** The rows of the social interactions screen in the panel's sepia rather than vanilla's grey. */
@Mixin(PlayerEntry.class)
abstract class PlayerEntryMixin {
	@Unique
	private static final int ROW = 0xFF4A3A36;
	@Unique
	private static final int ROW_REMOVED = 0xFF352A29;

	@ModifyArg(method = "extractContent", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), index = 4)
	private int cassonade$row(int color) {
		return ROW;
	}

	@ModifyArg(method = "extractContent", at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), index = 4)
	private int cassonade$rowRemoved(int color) {
		return ROW_REMOVED;
	}
}
