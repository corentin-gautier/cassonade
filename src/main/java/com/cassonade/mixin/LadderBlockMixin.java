package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import com.cassonade.ServerSupport;

/**
 * A ladder used on a ladder extends its column at the far end: upward, or downward when looking down,
 * as from the top of it. It stops where the spot is taken or the wall ends. Sneaking would skip the
 * block's use for the item's, so the look picks the way. Without the mod on the server, vanilla.
 */
@Mixin(LadderBlock.class)
public abstract class LadderBlockMixin extends Block {
	private LadderBlockMixin(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(asItem()) || !ServerSupport.has(player)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		Direction toward = player.getXRot() > 0 ? Direction.DOWN : Direction.UP;
		Direction facing = state.getValue(LadderBlock.FACING);
		BlockPos end = pos;
		while (level.isInsideBuildHeight(end.relative(toward)) && level.getBlockState(end.relative(toward)).is(this)
				&& level.getBlockState(end.relative(toward)).getValue(LadderBlock.FACING) == facing) {
			end = end.relative(toward);
		}
		BlockPos target = end.relative(toward);
		BlockState ladder = defaultBlockState().setValue(LadderBlock.FACING, facing)
				.setValue(LadderBlock.WATERLOGGED, level.getFluidState(target).is(Fluids.WATER));
		if (!level.isInsideBuildHeight(target) || !level.getBlockState(target).canBeReplaced() || !ladder.canSurvive(level, target)
				|| !player.mayUseItemAt(target, hit.getDirection(), stack)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (!level.isClientSide()) {
			level.setBlock(target, ladder, Block.UPDATE_ALL);
			SoundType sound = ladder.getSoundType();
			level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
			level.gameEvent(player, GameEvent.BLOCK_PLACE, target);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
