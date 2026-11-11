package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/** A raised shield sits lower in first person, so it hides less of what it blocks. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
	private static final float SHIELD_DROP = 0.2F;

	// The blocking animation's only check is whether the item is a shield: a shield being raised
	@ModifyExpressionValue(method = "submitArmWithItem", at = @At(value = "CONSTANT", args = "classValue=net/minecraft/world/item/ShieldItem"))
	private boolean cassonade$lowerShield(boolean shield, @Local(argsOnly = true) PoseStack poseStack) {
		if (shield && Settings.on(Setting.LOWER_SHIELD)) {
			poseStack.translate(0.0F, -SHIELD_DROP, 0.0F);
		}
		return shield;
	}
}
