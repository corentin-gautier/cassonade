package com.cassonade.stable;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cassonade.Cassonade;

/** The name typed for a horse in the screen of the post the player stands at; blank takes its name away. */
public record StableRenamePayload(BlockPos post, UUID horse, String name) implements CustomPacketPayload {
	public static final Type<StableRenamePayload> TYPE = new Type<>(Cassonade.id("stable_rename"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StableRenamePayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, StableRenamePayload::post,
			UUIDUtil.STREAM_CODEC, StableRenamePayload::horse,
			ByteBufCodecs.STRING_UTF8, StableRenamePayload::name,
			StableRenamePayload::new);

	@Override
	public Type<StableRenamePayload> type() {
		return TYPE;
	}
}
