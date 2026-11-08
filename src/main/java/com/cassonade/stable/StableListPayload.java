package com.cassonade.stable;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;

import com.cassonade.Cassonade;

/**
 * The player's horses, sent when a hitching post is used and after each thing done in its screen, which opens on
 * receiving this or updates if open. {@code candidate}: the network id of the nearest horse of theirs not registered yet,
 * or -1.
 */
public record StableListPayload(BlockPos post, List<Horse> horses, int candidate) implements CustomPacketPayload {
	public static final Type<StableListPayload> TYPE = new Type<>(Cassonade.id("stable_list"));

	/** Where a registered horse is, as the post's screen tells it. */
	public enum Status {
		/** Kept at the stable, out of the world: taken out at any post. */
		BOARDED,
		/** Around this post: it can be boarded. */
		HERE,
		/** Somewhere else in the world, loaded or not: called back to this post. */
		AWAY
	}

	/**
	 * {@code entity}: its UUID in the world, for the screen to follow it coming and going while open. {@code horse}: the
	 * horse as saved, for the screen to draw it and read its stats.
	 */
	public record Horse(UUID id, UUID entity, String name, Status status, TypedEntityData<EntityType<?>> horse) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Horse> STREAM_CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Horse::id,
				UUIDUtil.STREAM_CODEC, Horse::entity,
				ByteBufCodecs.STRING_UTF8, Horse::name,
				ByteBufCodecs.idMapper(index -> Status.values()[index], Status::ordinal), Horse::status,
				TypedEntityData.streamCodec(EntityType.STREAM_CODEC), Horse::horse,
				Horse::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, StableListPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, StableListPayload::post,
			Horse.STREAM_CODEC.apply(ByteBufCodecs.list()), StableListPayload::horses,
			ByteBufCodecs.VAR_INT, StableListPayload::candidate,
			StableListPayload::new);

	@Override
	public Type<StableListPayload> type() {
		return TYPE;
	}
}
