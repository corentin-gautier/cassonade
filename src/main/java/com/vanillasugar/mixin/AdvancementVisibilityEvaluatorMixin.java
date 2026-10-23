package com.vanillasugar.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;

/**
 * Every advancement with a display is sent to the player, not only those near the ones made, so the
 * client can list everything there is to do and guide the player to it. Hidden ones are sent too,
 * and the client keeps them secret until made. Recipe unlocks, which have no display, stay unsent.
 */
@Mixin(AdvancementVisibilityEvaluator.class)
public abstract class AdvancementVisibilityEvaluatorMixin {
	@WrapOperation(method = "evaluateVisibility", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;accept(Lnet/minecraft/advancements/AdvancementNode;Z)V"))
	private static void vanillasugar$showAll(AdvancementVisibilityEvaluator.Output output, AdvancementNode node, boolean visible,
			Operation<Void> original) {
		original.call(output, node, visible || node.advancement().display().isPresent());
	}
}
