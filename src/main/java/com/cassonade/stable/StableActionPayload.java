package com.cassonade.stable;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cassonade.Cassonade;

/**
 * A button of the post's screen: {@code horse} is the entry's id, or for REGISTER, the UUID of the horse in the world.
 * The server checks it all again and answers with the list.
 */
public record StableActionPayload(BlockPos post, Action action, UUID horse) implements CustomPacketPayload {
	public static final Type<StableActionPayload> TYPE = new Type<>(Cassonade.id("stable_action"));

	public enum Action {
		REGISTER,
		/** Into the stable, out of the world. */
		BOARD,
		/** Out of the stable, or back from wherever it is, at the post. */
		TAKE_OUT
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, StableActionPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, StableActionPayload::post,
			ByteBufCodecs.idMapper(index -> Action.values()[index], Action::ordinal), StableActionPayload::action,
			UUIDUtil.STREAM_CODEC, StableActionPayload::horse,
			StableActionPayload::new);

	@Override
	public Type<StableActionPayload> type() {
		return TYPE;
	}
}
