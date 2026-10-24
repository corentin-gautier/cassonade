package com.vanillasugar.combat;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.gear.PlayerGear;
import com.vanillasugar.item.VanillaSugarItems;

/**
 * The mobs chasing a player wearing a sculk compass, by entity id, so the client can show they spotted
 * them. A mob's target stays on the server; brain-driven mobs (piglins, hoglins, wardens...) answer
 * getTarget too. Sent again only when the list changes, and from scratch when the compass is worn again.
 */
public record HuntersPayload(List<Integer> mobs) implements CustomPacketPayload {
	public static final Type<HuntersPayload> TYPE = new Type<>(VanillaSugar.id("hunters"));
	public static final StreamCodec<RegistryFriendlyByteBuf, HuntersPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), HuntersPayload::mobs,
			HuntersPayload::new);
	/** Often enough that the "!" pops as the mob turns on the player, not a moment later. */
	private static final int SEND_INTERVAL = 2;
	/** The farthest a mob attacks from: a ghast's fireball. */
	public static final double RANGE = 64;
	private static final Map<ServerPlayer, HuntersPayload> SENT = new WeakHashMap<>();

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % SEND_INTERVAL != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!ServerPlayNetworking.canSend(player, TYPE)) {
					continue;
				}
				if (!PlayerGear.isWearing(player, VanillaSugarItems.SCULK_COMPASS)) {
					SENT.remove(player);
					continue;
				}
				HuntersPayload payload = new HuntersPayload(player.level()
						.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RANGE), mob -> mob.getTarget() == player)
						.stream().map(Entity::getId).sorted().toList());
				if (!payload.equals(SENT.put(player, payload))) {
					ServerPlayNetworking.send(player, payload);
				}
			}
		});
	}

	@Override
	public Type<HuntersPayload> type() {
		return TYPE;
	}
}
