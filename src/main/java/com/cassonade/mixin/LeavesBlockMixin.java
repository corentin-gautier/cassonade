package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Leaves cut off from their log decay as soon as their distance reaches 7, instead of waiting for a random tick. */
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
	@Shadow
	protected abstract void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random);

	@Inject(method = "tick", at = @At("TAIL"))
	private void cassonade$decayAtOnce(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		BlockState updated = level.getBlockState(pos);
		if (updated.is(state.getBlock())) {
			randomTick(updated, level, pos, random);
		}
	}
}
