package com.vanillasugar.clock;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.time.LongDay;

/**
 * How many nights the player has gone without sleeping, sent every second to the players wearing a clock:
 * the time since the last rest is a statistic the server keeps. Counted in the mod's longer days, as
 * phantoms count it (PhantomSpawnerMixin): they come from the third night on.
 */
public record Tiredness(int nights) implements CustomPacketPayload {
	public static final Type<Tiredness> TYPE = new Type<>(VanillaSugar.id("tiredness"));
	public static final StreamCodec<RegistryFriendlyByteBuf, Tiredness> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(Tiredness::new, Tiredness::nights);
	/** From this many nights without sleep, phantoms may come. */
	public static final int PHANTOM_NIGHTS = 3;
	private static final int SEND_INTERVAL = 20;

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % SEND_INTERVAL != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (WeatherForecast.hasClock(player) && ServerPlayNetworking.canSend(player, TYPE)) {
					int sinceRest = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
					ServerPlayNetworking.send(player, new Tiredness(sinceRest / LongDay.CYCLE_TICKS));
				}
			}
		});
	}

	@Override
	public Type<Tiredness> type() {
		return TYPE;
	}
}
