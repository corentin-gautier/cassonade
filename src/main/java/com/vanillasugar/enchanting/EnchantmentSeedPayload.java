package com.vanillasugar.enchanting;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import com.vanillasugar.VanillaSugar;

/**
 * The enchantment seed of an open enchanting table, so the client can list every enchantment an
 * option gives. Vanilla syncs the seed as a menu data slot, which the network cuts to 16 bits: the
 * client cannot draw the same enchantments from it.
 */
public record EnchantmentSeedPayload(int containerId, int seed) implements CustomPacketPayload {
	public static final Type<EnchantmentSeedPayload> TYPE = new Type<>(VanillaSugar.id("enchantment_seed"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EnchantmentSeedPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, EnchantmentSeedPayload::containerId,
			ByteBufCodecs.INT, EnchantmentSeedPayload::seed,
			EnchantmentSeedPayload::new);
	/** The last seed sent to each player, sent again only when it or the menu changes. */
	private static final Map<ServerPlayer, EnchantmentSeedPayload> SENT = new WeakHashMap<>();

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.containerMenu instanceof EnchantmentMenu menu && ServerPlayNetworking.canSend(player, TYPE)) {
					EnchantmentSeedPayload payload = new EnchantmentSeedPayload(menu.containerId, menu.getEnchantmentSeed());
					if (!payload.equals(SENT.put(player, payload))) {
						ServerPlayNetworking.send(player, payload);
					}
				}
			}
		});
	}

	/** What an option of the table gives, drawn as EnchantmentMenu.getEnchantmentList does. */
	public static List<EnchantmentInstance> enchantments(RegistryAccess access, ItemStack itemStack, int seed, int option, int cost) {
		RandomSource random = RandomSource.create(seed + option);
		Optional<HolderSet.Named<Enchantment>> tag = access.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
		if (tag.isEmpty()) {
			return List.of();
		}

		List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(random, itemStack, cost, tag.get().stream());
		if (itemStack.is(Items.BOOK) && list.size() > 1) {
			list.remove(random.nextInt(list.size()));
		}
		return list;
	}

	@Override
	public Type<EnchantmentSeedPayload> type() {
		return TYPE;
	}
}
