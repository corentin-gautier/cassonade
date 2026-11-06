package com.cassonade.waystone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cassonade.Cassonade;

/** A waystone moved in the list shown at the one the player stands at: a row up or down, or to the top or bottom. */
public record WaystoneMovePayload(BlockPos from, GlobalPos which, boolean up, boolean toEnd) implements CustomPacketPayload {
	public static final Type<WaystoneMovePayload> TYPE = new Type<>(Cassonade.id("waystone_move"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WaystoneMovePayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, WaystoneMovePayload::from,
			GlobalPos.STREAM_CODEC, WaystoneMovePayload::which,
			ByteBufCodecs.BOOL, WaystoneMovePayload::up,
			ByteBufCodecs.BOOL, WaystoneMovePayload::toEnd,
			WaystoneMovePayload::new);

	@Override
	public Type<WaystoneMovePayload> type() {
		return TYPE;
	}
}
