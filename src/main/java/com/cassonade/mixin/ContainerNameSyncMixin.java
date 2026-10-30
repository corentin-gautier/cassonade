package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Chests, barrels and Shulker boxes send their client the name given at the anvil, and nothing else of them (their contents
 * stay on the server), so that looking at one shows it (GlancePanel). Vanilla sends them nothing; a vanilla
 * client reads the name and has no use for it.
 */
@Mixin({ChestBlockEntity.class, BarrelBlockEntity.class, ShulkerBoxBlockEntity.class})
public abstract class ContainerNameSyncMixin extends BaseContainerBlockEntity {
	private ContainerNameSyncMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.storeNullable("CustomName", ComponentSerialization.CODEC, registries.createSerializationContext(NbtOps.INSTANCE), getCustomName());
		return tag;
	}
}
