package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;

/** Opening or closing one door of a double door does the same to the other. */
@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("RETURN"))
	private void cassonade$openDoubleDoor(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue() != InteractionResult.SUCCESS) {
			return;
		}
		BlockState door = level.getBlockState(pos);
		if (!door.is(state.getBlock())) {
			return;
		}
		// Doors placed side by side take opposite hinges, so the other one is on the side away from this one's hinge
		Direction facing = door.getValue(DoorBlock.FACING);
		DoorHingeSide hinge = door.getValue(DoorBlock.HINGE);
		BlockPos otherPos = pos.relative(hinge == DoorHingeSide.RIGHT ? facing.getCounterClockWise() : facing.getClockWise());
		BlockState other = level.getBlockState(otherPos);
		if (other.is(door.getBlock()) && other.getValue(DoorBlock.FACING) == facing && other.getValue(DoorBlock.HINGE) != hinge
				&& other.getValue(DoorBlock.HALF) == door.getValue(DoorBlock.HALF)) {
			((DoorBlock) door.getBlock()).setOpen(player, level, other, otherPos, door.getValue(DoorBlock.OPEN));
		}
	}
}
