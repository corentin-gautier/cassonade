package com.cassonade.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;

import com.cassonade.combat.Poison;
import com.cassonade.gear.ElytraGear;
import com.cassonade.storage.HandRefill;
import com.cassonade.tweaks.DivingMask;
import com.cassonade.tweaks.MurkyWater;
import com.cassonade.tweaks.PathSpeed;

/** Players glide with the elytra of their gear slot too, see {@link ElytraGear}. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** Faster on dirt paths, see {@link PathSpeed}. */
	@Inject(method = "baseTick", at = @At("TAIL"))
	private void cassonade$pathSpeed(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.level().isClientSide()) {
			PathSpeed.tick(self);
		}
	}

	/** Monsters notice someone later through the water, see {@link MurkyWater}. */
	@Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
	private void cassonade$murkyWater(ServerLevel level, Entity targetingEntity, CallbackInfoReturnable<Double> cir) {
		if (targetingEntity != null) {
			cir.setReturnValue(MurkyWater.visibility(cir.getReturnValue(), (LivingEntity) (Object) this, targetingEntity));
		}
	}

	/** A diving mask's snorkel out of the water lets its wearer breathe, see {@link DivingMask}. */
	@Inject(method = "canBreatheUnderwater", at = @At("RETURN"), cancellable = true)
	private void cassonade$snorkel(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && DivingMask.breathes((LivingEntity) (Object) this)) {
			cir.setReturnValue(true);
		}
	}

	/** Poison lasts half as long, see {@link Poison}. */
	@ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
	private MobEffectInstance cassonade$shortPoison(MobEffectInstance effect) {
		return Poison.shorten(effect);
	}

	@Inject(method = "canGlide", at = @At("RETURN"), cancellable = true)
	private void cassonade$glideWithGearElytra(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && (Object) this instanceof Player player && !player.onGround() && !player.isPassenger()
				&& !player.hasEffect(MobEffects.LEVITATION) && ElytraGear.canGlide(player)) {
			cir.setReturnValue(true);
		}
	}

	/** With no glider in the equipment slots, the one gliding is in the gear slot: it wears down instead. */
	@Inject(method = "updateFallFlying", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/Util;getRandom(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/lang/Object;"), cancellable = true)
	private void cassonade$hurtGearElytra(CallbackInfo ci, @Local(name = "slotsWithGliders") List<EquipmentSlot> slotsWithGliders) {
		if (slotsWithGliders.isEmpty() && (Object) this instanceof ServerPlayer player) {
			ElytraGear.hurt(player);
			player.gameEvent(GameEvent.ELYTRA_GLIDE);
			ci.cancel();
		}
	}

	/** A player's last food or item used up by holding the use button is replaced from the inventory, see {@link HandRefill}. */
	@WrapMethod(method = "completeUsingItem")
	private void cassonade$refillAfterUsing(Operation<Void> original) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity instanceof Player player && entity.isUsingItem()) {
			HandRefill.run(player, entity.getUsedItemHand(), () -> original.call());
		} else {
			original.call();
		}
	}
}
