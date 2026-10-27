package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.storage.HandRefill;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Shadow
	private int sleepCounter;

	/** The sleep timer runs twice as fast: the night skips after 50 ticks in bed instead of 100. */
	@Inject(method = "tick", at = @At("HEAD"))
	private void cassonade$fallAsleepFaster(CallbackInfo ci) {
		if (((Player) (Object) this).isSleeping() && sleepCounter < Player.SLEEP_DURATION) {
			sleepCounter++;
		}
	}

	/** Gear drops on death like the rest of the inventory, unless keepInventory is on. */
	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void cassonade$dropGear(ServerLevel level, CallbackInfo ci) {
		if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}

		Player player = (Player) (Object) this;
		PlayerGear gear = PlayerGear.of(player);
		for (GearSlot slot : GearSlot.values()) {
			ItemStack stack = gear.get(slot);
			if (!stack.isEmpty()) {
				ItemEntity drop = player.createItemStackToDrop(stack, true, false);
				if (drop != null) {
					level.addFreshEntity(drop);
				}
			}
		}
		player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.EMPTY);
	}

	/** A sweep spares the player's own animals (a wolf at their side, their horse) like it spares their teammates. */
	@WrapOperation(method = "doSweepAttack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean cassonade$spareOwnAnimals(Player player, Entity nearby, Operation<Boolean> original) {
		return nearby instanceof OwnableEntity animal && animal.getOwner() == player || original.call(player, nearby);
	}

	/** A weapon broken on a hit is replaced from the inventory, see {@link HandRefill}. */
	@WrapMethod(method = "attack")
	private void cassonade$refillAfterAttack(Entity entity, Operation<Void> original) {
		HandRefill.run((Player) (Object) this, InteractionHand.MAIN_HAND, () -> original.call(entity));
	}

	/** So is the last item given to an animal or used on an entity. */
	@WrapMethod(method = "interactOn")
	private InteractionResult cassonade$refillAfterInteract(Entity entity, InteractionHand hand, Vec3 location, Operation<InteractionResult> original) {
		return HandRefill.around((Player) (Object) this, hand, () -> original.call(entity, hand, location));
	}
}
