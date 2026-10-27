package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Players no longer trample farmland, on foot or riding: a jump in the field shouldn't undo the
 * tilling. Mobs still do, under the mobGriefing rule as in vanilla.
 */
@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {
	@WrapOperation(method = "fallOn", at = @At(value = "INVOKE", target = "turnToBaseBlock"))
	private void cassonade$notByPlayers(FarmlandBlock block, Entity entity, BlockState state, Level level, BlockPos pos, Operation<Void> original) {
		if (entity instanceof Player || entity.getControllingPassenger() instanceof Player) {
			return;
		}
		original.call(block, entity, state, level, pos);
	}
}
