package com.cassonade.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Shears on an item frame holding an item hide the frame, leaving the item alone on the wall, and show it
 * again the next time. An empty frame takes the shears as usual, and a hidden frame shows again once its
 * item is taken out, so it is never lost on the wall. Glow item frames too.
 */
@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
	@Shadow
	public abstract ItemStack getItem();

	@Shadow
	private boolean fixed;

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void cassonade$shearFrame(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
		ItemFrame frame = (ItemFrame) (Object) this;
		ItemStack stack = player.getItemInHand(hand);
		// The client already answers SUCCESS for a frame holding an item, as when rotating it
		if (!stack.is(Items.SHEARS) || getItem().isEmpty() || fixed || frame.level().isClientSide()) {
			return;
		}
		frame.setInvisible(!frame.isInvisible());
		frame.playSound(SoundEvents.SHEARS_SNIP, 1.0F, 1.0F);
		frame.gameEvent(GameEvent.SHEAR, player);
		stack.hurtAndBreak(1, player, hand);
		cir.setReturnValue(InteractionResult.SUCCESS);
	}

	@Inject(method = "dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V", at = @At("TAIL"))
	private void cassonade$showEmptied(ServerLevel level, @Nullable Entity causedBy, boolean withFrame, CallbackInfo ci) {
		ItemFrame frame = (ItemFrame) (Object) this;
		if (getItem().isEmpty() && frame.isInvisible()) {
			frame.setInvisible(false);
		}
	}
}
