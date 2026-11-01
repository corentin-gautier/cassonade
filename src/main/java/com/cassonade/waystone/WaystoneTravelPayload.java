package com.cassonade.waystone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cassonade.Cassonade;

/** A destination picked from the list, from the waystone the player stands at. */
public record WaystoneTravelPayload(BlockPos from, GlobalPos to) implements CustomPacketPayload {
	public static final Type<WaystoneTravelPayload> TYPE = new Type<>(Cassonade.id("waystone_travel"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WaystoneTravelPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, WaystoneTravelPayload::from,
			GlobalPos.STREAM_CODEC, WaystoneTravelPayload::to,
			WaystoneTravelPayload::new);

	@Override
	public Type<WaystoneTravelPayload> type() {
		return TYPE;
	}
}
