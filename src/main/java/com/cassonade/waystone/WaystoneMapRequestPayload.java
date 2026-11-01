package com.cassonade.waystone;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cassonade.Cassonade;

/** Asks for the waystones to show on the world map, as it opens. */
public record WaystoneMapRequestPayload() implements CustomPacketPayload {
	public static final Type<WaystoneMapRequestPayload> TYPE = new Type<>(Cassonade.id("waystone_map_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WaystoneMapRequestPayload> STREAM_CODEC = StreamCodec.unit(new WaystoneMapRequestPayload());

	@Override
	public Type<WaystoneMapRequestPayload> type() {
		return TYPE;
	}
}
