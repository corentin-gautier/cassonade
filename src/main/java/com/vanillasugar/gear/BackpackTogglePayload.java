package com.vanillasugar.gear;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.vanillasugar.VanillaSugar;

/** Sent by the client when the backpack panel is opened or closed. */
public record BackpackTogglePayload(boolean open) implements CustomPacketPayload {
	public static final Type<BackpackTogglePayload> TYPE = new Type<>(VanillaSugar.id("backpack_toggle"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BackpackTogglePayload> STREAM_CODEC =
			ByteBufCodecs.BOOL.<RegistryFriendlyByteBuf>cast().map(BackpackTogglePayload::new, BackpackTogglePayload::open);

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) ->
				((BackpackMenu) context.player().inventoryMenu).vanillasugar$backpack().setOpen(payload.open()));
	}

	@Override
	public Type<BackpackTogglePayload> type() {
		return TYPE;
	}
}
