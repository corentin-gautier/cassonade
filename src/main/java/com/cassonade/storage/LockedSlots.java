package com.cassonade.storage;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

import com.cassonade.Cassonade;

/**
 * The inventory slots a player has locked, hotbar included, one bit each: sorting and stashing leave
 * them alone, and a shift click neither takes from them nor fills them when empty. They can still be
 * filled or emptied by hand, and the hand refilled as usual. Stored on the player, kept on death and
 * synced to its own client, which toggles them with a click, the lock key held.
 */
public record LockedSlots(long mask) {
	public static final LockedSlots NONE = new LockedSlots(0);

	public static final AttachmentType<LockedSlots> ATTACHMENT = AttachmentRegistry.create(Cassonade.id("locked_slots"), builder -> builder
			.persistent(Codec.LONG.xmap(LockedSlots::new, LockedSlots::mask))
			.copyOnDeath()
			.syncWith(ByteBufCodecs.VAR_LONG.map(LockedSlots::new, LockedSlots::mask).cast(), AttachmentSyncPredicate.targetOnly()));

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Toggle.TYPE, Toggle.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Toggle.TYPE, (payload, context) -> {
			if (payload.slot() >= 0 && payload.slot() < Inventory.INVENTORY_SIZE) {
				context.player().setAttached(ATTACHMENT, of(context.player()).toggled(payload.slot()));
			}
		});
	}

	public static LockedSlots of(Player player) {
		return player.getAttachedOrElse(ATTACHMENT, NONE);
	}

	/** Whether the menu's slot is one of the player's inventory slots, locked. */
	public static boolean isLocked(Slot slot) {
		return slot.container instanceof Inventory inventory && of(inventory.player).isLocked(slot.getContainerSlot());
	}

	/** Whether the inventory slot (0 to 8 the hotbar, 9 to 35 above it) is locked. */
	public boolean isLocked(int inventorySlot) {
		return inventorySlot >= 0 && inventorySlot < Inventory.INVENTORY_SIZE && (mask & 1L << inventorySlot) != 0;
	}

	public LockedSlots toggled(int inventorySlot) {
		return new LockedSlots(mask ^ 1L << inventorySlot);
	}

	/** Sent by a click on one of the player's inventory slots, the lock key held: locks it, or unlocks it. */
	public record Toggle(int slot) implements CustomPacketPayload {
		public static final Type<Toggle> TYPE = new Type<>(Cassonade.id("lock_slot"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Toggle> STREAM_CODEC =
				ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(Toggle::new, Toggle::slot);

		@Override
		public Type<Toggle> type() {
			return TYPE;
		}
	}
}
