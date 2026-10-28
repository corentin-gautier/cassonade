package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;

import com.cassonade.tweaks.TreeClearance;

/** Most trees grow three blocks taller; one in eight keeps vanilla height. */
@Mixin(TrunkPlacer.class)
public abstract class TrunkPlacerMixin {
	@ModifyReturnValue(method = "getTreeHeight", at = @At("RETURN"))
	private int cassonade$tallerTrunks(int height, RandomSource random) {
		return TreeClearance.adjustHeight(height, random);
	}
}
