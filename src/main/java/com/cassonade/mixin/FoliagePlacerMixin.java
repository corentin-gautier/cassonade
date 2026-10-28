package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

import com.cassonade.tweaks.TreeClearance;

/** Hanging foliage is still refused in the three blocks above the sapling, so the path stays clear. */
@Mixin(FoliagePlacer.class)
public abstract class FoliagePlacerMixin {
	@Inject(method = "tryPlaceLeaf", at = @At("HEAD"), cancellable = true)
	private static void cassonade$noLowLeaves(WorldGenLevel level, FoliagePlacer.FoliageSetter foliageSetter, RandomSource random,
			TreeFeature tree, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (!TreeClearance.allowLeaf(pos)) {
			cir.setReturnValue(false);
		}
	}
}
