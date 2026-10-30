package com.cassonade.ping;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;

/**
 * A spot or a mob a player points out to the others, Shift and a middle click on it: sent to the server as a
 * {@link Request}, then to every player with the mod in the same dimension, the one who pointed included, who
 * see it marked for a moment. On a server without the mod, the middle click only picks the block, as in vanilla.
 */
public record PingPayload(String player, Vec3 pos, int entity, Optional<UUID> entityUuid) implements CustomPacketPayload {
	private static final StreamCodec<ByteBuf, Optional<UUID>> OPTIONAL_UUID_CODEC = ByteBufCodecs.STRING_UTF8.map(
			value -> value.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(value)),
			value -> value.map(UUID::toString).orElse(""));
	public static final Type<PingPayload> TYPE = new Type<>(Cassonade.id("ping"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PingPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, PingPayload::player,
			Vec3.STREAM_CODEC, PingPayload::pos,
			ByteBufCodecs.VAR_INT, PingPayload::entity,
			OPTIONAL_UUID_CODEC, PingPayload::entityUuid,
			PingPayload::new);
	/** No entity pointed out: a spot. */
	public static final int NO_ENTITY = -1;
	/** As far as a player can point: the client keeps the terrain and the mobs about that far. */
	public static final double RANGE = 128;
	/** A ping at most every half second per player, so a held click doesn't flood the others' screens. */
	private static final int INTERVAL = 10;
	private static final Map<ServerPlayer, Long> LAST = new WeakHashMap<>();

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (request, context) -> request.share(context.player()));
	}

	@Override
	public Type<PingPayload> type() {
		return TYPE;
	}

	public record Request(Vec3 pos, int entity) implements CustomPacketPayload {
		public static final Type<Request> TYPE = new Type<>(Cassonade.id("ping_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.composite(
				Vec3.STREAM_CODEC, Request::pos,
				ByteBufCodecs.VAR_INT, Request::entity,
				Request::new);

		private void share(ServerPlayer player) {
			long now = player.level().getGameTime();
			Long last = LAST.get(player);
			if (player.isSpectator() || last != null && now - last < INTERVAL) {
				return;
			}
			Entity pointed = entity == NO_ENTITY ? null : player.level().getEntity(entity);
			Vec3 at = pointed != null ? pointed.position() : pos;
			// A little past the range: the mob may have stepped away since the client sent it
			if (at.distanceToSqr(player.getEyePosition()) > (RANGE + 8) * (RANGE + 8)) {
				return;
			}
			LAST.put(player, now);
			PingPayload ping = new PingPayload(player.getScoreboardName(), at, pointed != null ? pointed.getId() : NO_ENTITY,
					Optional.ofNullable(pointed).map(Entity::getUUID));
			for (ServerPlayer other : player.level().players()) {
				if (ServerPlayNetworking.canSend(other, PingPayload.TYPE)) {
					ServerPlayNetworking.send(other, ping);
				}
			}
		}

		@Override
		public Type<Request> type() {
			return TYPE;
		}
	}
}
