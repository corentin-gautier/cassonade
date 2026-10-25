package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.gui.components.toasts.SystemToast;

/** System messages' titles, yellow in vanilla, in the quest tracker's gold like the other toasts. */
@Mixin(SystemToast.class)
public abstract class SystemToastMixin {
	/** The quest tracker's GOLD. */
	@Unique
	private static final int TITLE = 0xFFF6DA8A;

	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 0xFFFFFF00))
	private int cassonade$goldTitle(int color) {
		return TITLE;
	}
}
