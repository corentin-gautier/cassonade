package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import com.cassonade.hopper.HopperFilter;

/** Hoppers tell in their block state whether they have a filter, for assets/minecraft/blockstates/hopper.json. */
@Mixin(HopperBlock.class)
public abstract class HopperBlockMixin extends BaseEntityBlock {
	private HopperBlockMixin(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void cassonade$unfilteredByDefault(BlockBehaviour.Properties properties, CallbackInfo ci) {
		this.registerDefaultState(this.defaultBlockState().setValue(HopperFilter.FILTERED, false));
	}

	@Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
	private void cassonade$addFiltered(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
		builder.add(HopperFilter.FILTERED);
	}
}
