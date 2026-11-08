package com.cassonade.lighthouse;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cassonade.Cassonade;

/** The lamps of the lighthouses within sight of the player, in their dimension: all of them each time, none to clear. */
public record LighthousesPayload(List<BlockPos> lamps) implements CustomPacketPayload {
	public static final Type<LighthousesPayload> TYPE = new Type<>(Cassonade.id("lighthouses"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LighthousesPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), LighthousesPayload::lamps,
			LighthousesPayload::new);

	@Override
	public Type<LighthousesPayload> type() {
		return TYPE;
	}
}
