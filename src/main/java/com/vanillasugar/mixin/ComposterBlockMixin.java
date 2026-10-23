package com.vanillasugar.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.level.block.ComposterBlock;

/** Every layer a compostable item adds counts twice, so the composter fills in half the items. */
@Mixin(ComposterBlock.class)
public abstract class ComposterBlockMixin {
	@ModifyExpressionValue(
		method = "addLayer",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/loot/providers/number/ints/ResolvableInt;get(Lnet/minecraft/world/level/storage/loot/LootContext;I)I")
	)
	private static int vanillasugar$doubleLayers(int layers) {
		return layers * 2;
	}
}
