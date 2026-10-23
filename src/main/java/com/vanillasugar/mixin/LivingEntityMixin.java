package com.vanillasugar.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;

import com.vanillasugar.gear.ElytraGear;
import com.vanillasugar.storage.HandRefill;

/** Players glide with the elytra of their gear slot too, see {@link ElytraGear}. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "canGlide", at = @At("RETURN"), cancellable = true)
	private void vanillasugar$glideWithGearElytra(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && (Object) this instanceof Player player && !player.onGround() && !player.isPassenger()
				&& !player.hasEffect(MobEffects.LEVITATION) && ElytraGear.canGlide(player)) {
			cir.setReturnValue(true);
		}
	}

	/** With no glider in the equipment slots, the one gliding is in the gear slot: it wears down instead. */
	@Inject(method = "updateFallFlying", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/Util;getRandom(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/lang/Object;"), cancellable = true)
	private void vanillasugar$hurtGearElytra(CallbackInfo ci, @Local(name = "slotsWithGliders") List<EquipmentSlot> slotsWithGliders) {
		if (slotsWithGliders.isEmpty() && (Object) this instanceof ServerPlayer player) {
			ElytraGear.hurt(player);
			player.gameEvent(GameEvent.ELYTRA_GLIDE);
			ci.cancel();
		}
	}

	/** A player's last food or item used up by holding the use button is replaced from the inventory, see {@link HandRefill}. */
	@WrapMethod(method = "completeUsingItem")
	private void vanillasugar$refillAfterUsing(Operation<Void> original) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity instanceof Player player && entity.isUsingItem()) {
			HandRefill.run(player, entity.getUsedItemHand(), () -> original.call());
		} else {
			original.call();
		}
	}
}
