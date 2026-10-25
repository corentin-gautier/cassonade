package com.cassonade.compass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

/** A point placed by the player from the world map, drawn as a banner on the maps and in the world. The name may be empty. */
public record Marker(GlobalPos pos, DyeColor color, String name) {
	public static final int MAX_NAME_LENGTH = 32;

	public static final Codec<Marker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			GlobalPos.CODEC.fieldOf("pos").forGetter(Marker::pos),
			DyeColor.CODEC.fieldOf("color").forGetter(Marker::color),
			Codec.string(0, MAX_NAME_LENGTH).optionalFieldOf("name", "").forGetter(Marker::name)
	).apply(instance, Marker::new));

	public static final StreamCodec<ByteBuf, Marker> STREAM_CODEC = StreamCodec.composite(
			GlobalPos.STREAM_CODEC, Marker::pos,
			DyeColor.STREAM_CODEC, Marker::color,
			ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), Marker::name,
			Marker::new);
}
