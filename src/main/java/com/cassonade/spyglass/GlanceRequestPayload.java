package com.cassonade.spyglass;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import com.cassonade.Cassonade;
import com.cassonade.item.CassonadeItems;

/**
 * Asked by a player looking through a naturalist's spyglass about the mob they see, answered with a
 * {@link GlancePayload}: what only the server knows about it. Ignored without the spyglass in hand, or for a
 * mob out of its range.
 */
public record GlanceRequestPayload(int entity) implements CustomPacketPayload {
	public static final Type<GlanceRequestPayload> TYPE = new Type<>(Cassonade.id("glance_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GlanceRequestPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, GlanceRequestPayload::entity,
			GlanceRequestPayload::new);
	/** As far as the spyglass reads: about as far as a client keeps track of animals, past which it no longer knows them. */
	public static final double RANGE = 128;

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> payload.answer(context.player()));
	}

	private void answer(ServerPlayer player) {
		if (!player.isHolding(CassonadeItems.NATURALIST_SPYGLASS)) {
			return;
		}
		Entity looked = player.level().getEntity(entity);
		// A little past the range: the mob may have stepped away since the client asked
		if (looked != null && looked.distanceToSqr(player) <= (RANGE + 8) * (RANGE + 8)) {
			GlancePayload.of(looked).ifPresent(answer -> ServerPlayNetworking.send(player, answer));
		}
	}

	@Override
	public Type<GlanceRequestPayload> type() {
		return TYPE;
	}
}
