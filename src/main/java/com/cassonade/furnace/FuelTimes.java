package com.cassonade.furnace;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;

/**
 * How long each fuel burns in a furnace, in ticks, sent to each player on joining and after a data
 * pack reload. Burn times are data-driven providers the client cannot evaluate: they need a loot
 * context only the server builds. A blast furnace or a smoker burns fuel twice as fast but cooks twice
 * as fast too, so the furnace's times give the items smelted in all three.
 */
public record FuelTimes(Map<Holder<Item>, Integer> ticks) implements CustomPacketPayload {
	public static final Type<FuelTimes> TYPE = new Type<>(Cassonade.id("fuel_times"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FuelTimes> STREAM_CODEC =
			ByteBufCodecs.map(HashMap<Holder<Item>, Integer>::new, ByteBufCodecs.holderRegistry(Registries.ITEM), ByteBufCodecs.VAR_INT)
					.map(ticks -> new FuelTimes(ticks), times -> new HashMap<>(times.ticks()));
	/** What a furnace takes to smelt one item. */
	public static final int TICKS_PER_ITEM = 200;

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.player, of(server)));
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			FuelTimes times = of(server);
			server.getPlayerList().getPlayers().forEach(player -> send(player, times));
		});
	}

	private static void send(ServerPlayer player, FuelTimes times) {
		if (ServerPlayNetworking.canSend(player, TYPE)) {
			ServerPlayNetworking.send(player, times);
		}
	}

	/** Every fuel's burn time, read the way a furnace standing at the world's origin would. */
	static FuelTimes of(MinecraftServer server) {
		ServerLevel level = server.overworld();
		FurnaceBlockEntity furnace = new FurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
		LootContext context = new LootContext.Builder(new LootParams.Builder(level)
				.withParameter(LootContextParams.BLOCK_STATE, furnace.getBlockState())
				.withParameter(LootContextParams.BLOCK_ENTITY, furnace)
				.withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
				.withParameter(LootContextParams.CONTAINER, furnace)
				.create(LootContextParamSets.CONTAINER_PROCESS))
				.create(Optional.empty());
		Map<Holder<Item>, Integer> ticks = new HashMap<>();
		for (Item item : BuiltInRegistries.ITEM) {
			ItemStack stack = item.getDefaultInstance();
			if (stack.has(DataComponents.COOKING_FUEL)) {
				int burn = ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
				if (burn > 0) {
					ticks.put(BuiltInRegistries.ITEM.wrapAsHolder(item), burn);
				}
			}
		}
		return new FuelTimes(ticks);
	}

	@Override
	public Type<FuelTimes> type() {
		return TYPE;
	}
}
