package com.cassonade.waystone;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cassonade.Cassonade;

/** The name of the waystone used and the others, sent when one is used: the screen listing them opens on receiving this. */
public record WaystoneListPayload(BlockPos from, String name, boolean canRename, List<Destination> destinations) implements CustomPacketPayload {
	public static final Type<WaystoneListPayload> TYPE = new Type<>(Cassonade.id("waystone_list"));

	public record Destination(GlobalPos pos, String name) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Destination> STREAM_CODEC = StreamCodec.composite(
				GlobalPos.STREAM_CODEC, Destination::pos,
				ByteBufCodecs.STRING_UTF8, Destination::name,
				Destination::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, WaystoneListPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, WaystoneListPayload::from,
			ByteBufCodecs.STRING_UTF8, WaystoneListPayload::name,
			ByteBufCodecs.BOOL, WaystoneListPayload::canRename,
			Destination.STREAM_CODEC.apply(ByteBufCodecs.list()), WaystoneListPayload::destinations,
			WaystoneListPayload::new);

	@Override
	public Type<WaystoneListPayload> type() {
		return TYPE;
	}
}
