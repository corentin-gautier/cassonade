package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.renderer.GlobalSettingsUniform;

import com.cassonade.client.OpenFade;

/** The menu blur grows from nothing while a window opens, and back to nothing as it closes (see ScreenMixin). */
@Mixin(GlobalSettingsUniform.class)
public abstract class GlobalSettingsUniformMixin {
	@ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private int cassonade$blurIn(int radius) {
		return Math.round(radius * OpenFade.takeBlur());
	}
}
