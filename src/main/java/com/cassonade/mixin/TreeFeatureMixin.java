package com.cassonade.mixin;

import java.util.function.BiConsumer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BushFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

import com.cassonade.tweaks.TreeClearance;

/** Marks the tree being grown so the trunk can be lengthened and low leaves skipped, except bushes. */
@Mixin(TreeFeature.class)
public abstract class TreeFeatureMixin {
	@WrapMethod(method = "doPlace")
	private boolean cassonade$clearance(WorldGenLevel level, RandomSource random, BlockPos origin,
			BiConsumer<BlockPos, BlockState> rootSetter, BiConsumer<BlockPos, BlockState> trunkSetter,
			FoliagePlacer.FoliageSetter foliageSetter, Operation<Boolean> original) {
		TreeClearance.begin(origin, ((TreeFeature) (Object) this).foliagePlacer() instanceof BushFoliagePlacer);
		try {
			return original.call(level, random, origin, rootSetter, trunkSetter, foliageSetter);
		} finally {
			TreeClearance.end();
		}
	}
}
