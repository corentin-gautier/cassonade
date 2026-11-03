package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.cassonade.client.ambience.AfterRain;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockDripMixin {
	/** After the rain, vanilla's drops at its rate, fewer as the leaves dry, under any leaf the rain reached. */
	@Inject(method = "makeDrippingWaterParticles", at = @At("HEAD"))
	private static void cassonade$dripAfterRain(Level level, BlockPos pos, RandomSource random, BlockState belowState, BlockPos below, CallbackInfo ci) {
		if (!(level instanceof ClientLevel client)) {
			return;
		}
		float wetness = AfterRain.wetness(client);
		if (wetness == 0 || random.nextInt(15) != 1 || random.nextFloat() >= wetness) {
			return;
		}
		if ((!belowState.canOcclude() || !belowState.isFaceSturdy(level, below, Direction.UP)) && AfterRain.wasRainedOn(client, pos)) {
			ParticleUtils.spawnParticleBelow(level, pos, random, ParticleTypes.DRIPPING_WATER);
		}
	}
}
