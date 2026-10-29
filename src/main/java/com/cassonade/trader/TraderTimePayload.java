package com.cassonade.trader;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.inventory.MerchantMenu;

import com.cassonade.Cassonade;
import com.cassonade.mixin.MerchantMenuAccessor;

/** The time left for a wandering trader with its trade screen open, which only the server knows. */
public record TraderTimePayload(int containerId, int ticks) implements CustomPacketPayload {
	public static final Type<TraderTimePayload> TYPE = new Type<>(Cassonade.id("trader_time"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TraderTimePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TraderTimePayload::containerId,
			ByteBufCodecs.VAR_INT, TraderTimePayload::ticks,
			TraderTimePayload::new);
	private static final int SEND_INTERVAL = 20;

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % SEND_INTERVAL != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.containerMenu instanceof MerchantMenu menu
						&& ((MerchantMenuAccessor) menu).cassonade$getTrader() instanceof WanderingTrader trader
						&& ServerPlayNetworking.canSend(player, TYPE)) {
					ServerPlayNetworking.send(player, new TraderTimePayload(menu.containerId, trader.getDespawnDelay()));
				}
			}
		});
	}

	@Override
	public Type<TraderTimePayload> type() {
		return TYPE;
	}
}
