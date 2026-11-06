package com.cassonade.enchanting;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;

import com.cassonade.Cassonade;

/**
 * Drawing other offers at the enchanting table for levels: the first time 1, then 2, 4, 8… until the player enchants an
 * item, which brings it back to 1. It does what enchanting a cheap item does in vanilla to change the offers, without the
 * item and the lapis, and doubling, a few tries stay cheap while hunting for one exact enchantment soon costs too much. The menu's own button channel carries it (button 3, after
 * the three offers), so no packet of its own: a server without the mod refuses it.
 */
public final class EnchantmentReroll {
	public static final int BUTTON = 3;
	private static final int MAX_DOUBLINGS = 20;
	/** How many times the player drew again since they last enchanted something; kept through death, shown on the client. */
	public static final AttachmentType<Integer> REROLLS = AttachmentRegistry.create(Cassonade.id("enchantment_rerolls"), builder -> builder
			.persistent(Codec.INT)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	private EnchantmentReroll() {
	}

	public static void init() {
	}

	/** In levels: doubles with each draw, held under any level a player can reach. */
	public static int cost(Player player) {
		return 1 << Math.min(player.getAttachedOrElse(REROLLS, 0), MAX_DOUBLINGS);
	}

	/** With something to enchant on the table, and the levels to pay (or in Creative). */
	public static boolean canReroll(Player player, EnchantmentMenu menu) {
		boolean offers = menu.costs[0] > 0 || menu.costs[1] > 0 || menu.costs[2] > 0;
		return offers && (player.hasInfiniteMaterials() || player.experienceLevel >= cost(player));
	}

	/** Takes the levels and counts the draw; the player's new seed comes from the game's own enchanting. */
	public static void pay(Player player) {
		int cost = cost(player);
		player.onEnchantmentPerformed(ItemStack.EMPTY, player.hasInfiniteMaterials() ? 0 : cost);
		player.setAttached(REROLLS, player.getAttachedOrElse(REROLLS, 0) + 1);
	}

	/** An item really enchanted: the next draw costs 1 again. */
	public static void enchanted(Player player) {
		player.removeAttached(REROLLS);
	}
}
