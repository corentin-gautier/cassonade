package com.cassonade.gear;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import com.cassonade.ServerSupport;
import com.cassonade.Cassonade;

/**
 * An elytra worn in its gear slot glides like one worn as a chestplate, so both can be worn at once.
 * Gliding wears it down and Mending repairs it (see LivingEntityMixin and EnchantmentHelperMixin);
 * using an elytra puts it in this slot rather than in place of the chestplate.
 */
public final class ElytraGear {
	/**
	 * A copy of the worn elytra that every player around receives, to draw the wings: the gear itself
	 * only goes to its owner, the backpack's contents with it.
	 */
	public static final AttachmentType<ItemStack> SHOWN = AttachmentRegistry.create(Cassonade.id("shown_elytra"), builder -> builder
			.syncWith(ItemStack.OPTIONAL_STREAM_CODEC, AttachmentSyncPredicate.all()));

	private ElytraGear() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				sync(player);
			}
		});
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack inHand = player.getItemInHand(hand);
			if (!GearSlot.ELYTRA.accepts(inHand) || player.isSpectator() || !ServerSupport.has(player)) {
				return InteractionResult.PASS;
			}
			return equip(player, inHand);
		});
	}

	public static ItemStack worn(Player player) {
		return PlayerGear.of(player).elytra();
	}

	/** The elytra other players see on this one, synced to every client. */
	public static ItemStack shown(Player player) {
		return player.getAttachedOrElse(SHOWN, ItemStack.EMPTY);
	}

	public static boolean canGlide(Player player) {
		return LivingEntity.canGlideUsing(worn(player), EquipmentSlot.CHEST);
	}

	/** Wears the elytra down while gliding, as vanilla does to a worn one. */
	public static void hurt(ServerPlayer player) {
		worn(player).hurtAndBreak(1, player.level(), player, item -> {
		});
	}

	/** Swaps the elytra in hand with the one in the gear slot, like vanilla equips armor. */
	private static InteractionResult equip(Player player, ItemStack inHand) {
		PlayerGear gear = PlayerGear.of(player);
		ItemStack worn = gear.elytra();
		if (ItemStack.isSameItemSameComponents(inHand, worn)) {
			return InteractionResult.FAIL;
		}

		ItemStack toHand = worn.isEmpty() ? inHand : worn;
		ItemStack toGear = player.isCreative() ? inHand.copy() : inHand.copyAndClear();
		player.setAttached(PlayerGear.ATTACHMENT, gear.with(GearSlot.ELYTRA, toGear));
		Equippable equippable = toGear.get(DataComponents.EQUIPPABLE);
		if (equippable != null && !player.level().isClientSide()) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), equippable.equipSound(), player.getSoundSource(), 1, 1);
		}
		return InteractionResult.SUCCESS.heldItemTransformedTo(toHand);
	}

	/**
	 * Sends the worn elytra when it changed: to everyone around for the wings, and again to its owner
	 * when it changed in place, worn down or repaired, which re-setting the same gear would not send.
	 */
	private static void sync(ServerPlayer player) {
		ItemStack worn = worn(player);
		if (ItemStack.matches(worn, shown(player))) {
			return;
		}

		if (worn.isEmpty()) {
			player.removeAttached(SHOWN);
		} else {
			player.setAttached(SHOWN, worn.copy());
		}
		player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.ELYTRA, worn.copy()));
	}
}
