package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Beehives tell their clients which bees are inside, vanilla sending them nothing, so that looking at one through
 * the naturalist's spyglass shows how many (GlancePanel). Sent when the count changes. A vanilla client ignores it.
 */
@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveSyncMixin extends BlockEntity {
	@Unique
	private int cassonade$sentCount = -1;

	private BeehiveSyncMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Inject(method = "serverTick", at = @At("TAIL"))
	private static void cassonade$syncCount(Level level, BlockPos pos, BlockState state, BeehiveBlockEntity hive, CallbackInfo ci) {
		BeehiveSyncMixin sync = (BeehiveSyncMixin) (Object) hive;
		int count = hive.getOccupantCount();
		if (sync.cassonade$sentCount != count) {
			// The first tick sends nothing: the chunk's own packet carries the bees as they are
			if (sync.cassonade$sentCount != -1) {
				level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
			}
			sync.cassonade$sentCount = count;
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}
