package com.cassonade.waystone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cassonade.Cassonade;

/** The name typed in the screen of the waystone the player stands at. */
public record WaystoneRenamePayload(BlockPos pos, String name) implements CustomPacketPayload {
	public static final Type<WaystoneRenamePayload> TYPE = new Type<>(Cassonade.id("waystone_rename"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WaystoneRenamePayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, WaystoneRenamePayload::pos,
			ByteBufCodecs.STRING_UTF8, WaystoneRenamePayload::name,
			WaystoneRenamePayload::new);

	@Override
	public Type<WaystoneRenamePayload> type() {
		return TYPE;
	}
}
