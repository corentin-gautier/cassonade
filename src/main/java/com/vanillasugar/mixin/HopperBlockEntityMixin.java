package com.vanillasugar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.vanillasugar.hopper.HopperFilter;
import com.vanillasugar.hopper.HopperFilterMenu;

/**
 * Applies the hopper's filter, see {@link HopperFilter}. Hoppers move items in through canPlaceItem, whether they take them
 * from the container above, pick them up or are fed by another hopper: refusing the items the filter doesn't match is enough.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends RandomizableContainerBlockEntity {
	private HopperBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return HopperFilter.accepts(HopperFilter.filter(this), stack);
	}

	/** The filter is a real item: it drops with the hopper's contents. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		ItemStack filter = HopperFilter.filter(this);
		if (this.level != null && !filter.isEmpty()) {
			Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), filter);
		}
	}

	@Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
	private void vanillasugar$filterMenu(int containerId, Inventory inventory, CallbackInfoReturnable<AbstractContainerMenu> cir) {
		cir.setReturnValue(new HopperFilterMenu(containerId, inventory, (HopperBlockEntity) (Object) this));
	}
}
