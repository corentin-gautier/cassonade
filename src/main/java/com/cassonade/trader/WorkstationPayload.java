package com.cassonade.trader;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;

import com.cassonade.Cassonade;

/** Where a villager works, sent as its trade screen opens: a villager's memory never reaches the client. */
public record WorkstationPayload(int containerId, BlockPos pos) implements CustomPacketPayload {
	public static final Type<WorkstationPayload> TYPE = new Type<>(Cassonade.id("workstation"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WorkstationPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, WorkstationPayload::containerId,
			BlockPos.STREAM_CODEC, WorkstationPayload::pos,
			WorkstationPayload::new);

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
	}

	public static void send(ServerPlayer player, Villager villager) {
		if (ServerPlayNetworking.canSend(player, TYPE)) {
			villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
					.filter(site -> site.dimension() == villager.level().dimension())
					.ifPresent(site -> ServerPlayNetworking.send(player, new WorkstationPayload(player.containerMenu.containerId, site.pos())));
		}
	}

	@Override
	public Type<WorkstationPayload> type() {
		return TYPE;
	}
}
