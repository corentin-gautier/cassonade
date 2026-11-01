package com.cassonade.waystone;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cassonade.Cassonade;

/** Every waystone the player knows, for the world map and the minimap to mark those of the dimension they show. */
public record WaystoneMapPayload(List<WaystoneListPayload.Destination> waystones) implements CustomPacketPayload {
	public static final Type<WaystoneMapPayload> TYPE = new Type<>(Cassonade.id("waystone_map"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WaystoneMapPayload> STREAM_CODEC = StreamCodec.composite(
			WaystoneListPayload.Destination.STREAM_CODEC.apply(ByteBufCodecs.list()), WaystoneMapPayload::waystones,
			WaystoneMapPayload::new);

	@Override
	public Type<WaystoneMapPayload> type() {
		return TYPE;
	}
}
