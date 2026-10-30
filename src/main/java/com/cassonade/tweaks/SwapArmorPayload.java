package com.cassonade.tweaks;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;

import com.cassonade.Cassonade;

/** Alt and a right click with an empty hand on an armor stand: the server doesn't know about Alt, so the client asks. */
public record SwapArmorPayload(int stand) implements CustomPacketPayload {
	public static final Type<SwapArmorPayload> TYPE = new Type<>(Cassonade.id("swap_armor"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SwapArmorPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SwapArmorPayload::stand,
			SwapArmorPayload::new);
	private static final double REACH_MARGIN = 2;

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> payload.swap(context.player()));
	}

	private void swap(ServerPlayer player) {
		if (player.level().getEntity(stand) instanceof ArmorStand armorStand && !armorStand.isMarker() && !player.isSpectator()
				&& player.getMainHandItem().isEmpty() && player.isWithinEntityInteractionRange(armorStand, REACH_MARGIN)) {
			((ArmorSwapper) armorStand).cassonade$swapArmor(player);
		}
	}

	@Override
	public Type<SwapArmorPayload> type() {
		return TYPE;
	}
}
