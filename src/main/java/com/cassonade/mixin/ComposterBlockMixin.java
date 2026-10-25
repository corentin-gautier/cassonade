package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ComposterBlock;

/**
 * A full composter is ready at once, instead of a second later, and gives eight bone meal instead
 * of one, by hand or to a hopper below (HopperBlockEntityMixin takes them all at once).
 */
@Mixin(ComposterBlock.class)
public abstract class ComposterBlockMixin {
	@Unique
	private static final int BONE_MEAL = 8;

	// The tick turning the last layer into bone meal, scheduled 20 ticks after it is added
	@ModifyArg(method = {"addLayer", "onPlace"}, at = @At(value = "INVOKE", target = "scheduleTick"), index = 2)
	private static int cassonade$readyAtOnce(int delay) {
		return 1;
	}

	@ModifyExpressionValue(method = {"extractProduce", "getContainer"},
			at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/item/ItemStack;"))
	private static ItemStack cassonade$moreBoneMeal(ItemStack boneMeal) {
		return boneMeal.copyWithCount(BONE_MEAL);
	}
}
