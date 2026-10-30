package com.cassonade.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.cassonade.gear.BackpackContainer;
import com.cassonade.gear.OrganizedBackpack;

/** What is picked up first completes the stacks of an organized backpack; the rest goes to the inventory, as usual. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Shadow
	private int pickupDelay;

	@Shadow
	public abstract ItemStack getItem();

	@Shadow
	public abstract void setItem(ItemStack stack);

	@Shadow
	private UUID target;

	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void cassonade$topUpBackpack(Player player, CallbackInfo info) {
		ItemEntity entity = (ItemEntity) (Object) this;
		BackpackContainer backpack = OrganizedBackpack.of(player);
		if (player.level().isClientSide() || backpack == null || pickupDelay != 0 || target != null && !target.equals(player.getUUID())) {
			return;
		}
		ItemStack stack = getItem();
		int before = stack.getCount();
		OrganizedBackpack.topUp(backpack, stack);
		int moved = before - stack.getCount();
		if (moved == 0) {
			return;
		}
		player.take(entity, moved);
		player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), moved);
		if (stack.isEmpty()) {
			entity.discard();
			info.cancel();
		} else {
			setItem(stack);
		}
	}
}
