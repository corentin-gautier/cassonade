package com.vanillasugar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.vanillasugar.ComposterOutput;
import com.vanillasugar.hopper.HopperFilter;
import com.vanillasugar.hopper.HopperFilterMenu;

/**
 * Applies the hopper's filter, see {@link HopperFilter}. Hoppers move items in through canPlaceItem, whether they take them
 * from the container above, pick them up or are fed by another hopper: refusing the items the filter doesn't match is enough.
 * Hoppers also take a full composter's bone meal all at once (see ComposterBlockMixin).
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends RandomizableContainerBlockEntity {
	private HopperBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Shadow
	private static boolean canTakeItemFromContainer(Container hopper, Container container, ItemStack itemStack, int slot, Direction direction) {
		throw new AssertionError();
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

	/**
	 * A composter has no block entity to remember how much bone meal is left: a hopper takes vanilla's
	 * one at a time, which would empty the composter of the rest. It takes them all once it has room.
	 */
	@Inject(method = "tryTakeInItemFromSlot", at = @At("HEAD"), cancellable = true)
	private static void vanillasugar$takeAllBoneMeal(Hopper hopper, Container container, int slot, Direction direction,
			CallbackInfoReturnable<Boolean> cir) {
		if (!(container instanceof ComposterOutput)) {
			return;
		}
		ItemStack boneMeal = container.getItem(slot);
		if (boneMeal.isEmpty() || !canTakeItemFromContainer(hopper, container, boneMeal, slot, direction) || room(hopper, boneMeal) < boneMeal.getCount()) {
			cir.setReturnValue(false);
			return;
		}
		HopperBlockEntity.addItem(container, hopper, container.removeItem(slot, boneMeal.getCount()), null);
		container.setChanged();
		cir.setReturnValue(true);
	}

	/** How many of the stack the hopper has room for. */
	@Unique
	private static int room(Container hopper, ItemStack stack) {
		int room = 0;
		for (int i = 0; i < hopper.getContainerSize(); i++) {
			if (!hopper.canPlaceItem(i, stack)) {
				continue;
			}
			ItemStack inSlot = hopper.getItem(i);
			int max = Math.min(hopper.getMaxStackSize(stack), stack.getMaxStackSize());
			if (inSlot.isEmpty()) {
				room += max;
			} else if (ItemStack.isSameItemSameComponents(inSlot, stack)) {
				room += Math.max(0, max - inSlot.getCount());
			}
		}
		return room;
	}
}
