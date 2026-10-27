package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import com.cassonade.client.NewItems;
import com.cassonade.client.PickedUpItems;
import com.cassonade.client.combat.DamageDirection;

/** The local player's hits, with where they came from, for DamageDirection, and what they pick up, for PickedUpItems and NewItems. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@WrapOperation(method = "handleDamageEvent", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;handleDamageEvent(Lnet/minecraft/world/damagesource/DamageSource;)V"))
	private void cassonade$damageDirection(Entity entity, DamageSource source, Operation<Void> original) {
		original.call(entity, source);
		if (entity instanceof LocalPlayer player) {
			DamageDirection.hurt(player, source);
		}
	}

	// After the check that moves the packet to the game's thread: before it, this would run on both threads
	@Inject(method = "handleTakeItemEntity", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/network/protocol/PacketUtils;"
			+ "ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V"))
	private void cassonade$pickedUp(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		// Read before the game takes the amount off the item entity, further down
		if (minecraft.player != null && packet.getPlayerId() == minecraft.player.getId()
				&& minecraft.level.getEntity(packet.getItemId()) instanceof ItemEntity item) {
			PickedUpItems.add(item.getItem(), packet.getAmount());
			NewItems.pickedUp(item.getItem());
		}
	}
}
