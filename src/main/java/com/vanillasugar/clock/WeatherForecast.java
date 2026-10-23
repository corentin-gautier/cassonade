package com.vanillasugar.clock;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.gear.PlayerGear;

/**
 * The next change of weather, sent every second to the players carrying a clock: in how many ticks,
 * and the weather it brings. The client only knows the current weather, the timers stay on the server.
 * {@code ticks} is -1 when the next change is not known yet: the weather is frozen by the game rule,
 * or a timer is about to be drawn at random.
 */
public record WeatherForecast(int ticks, boolean raining, boolean thundering) implements CustomPacketPayload {
	public static final Type<WeatherForecast> TYPE = new Type<>(VanillaSugar.id("weather_forecast"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WeatherForecast> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, WeatherForecast::ticks,
			ByteBufCodecs.BOOL, WeatherForecast::raining,
			ByteBufCodecs.BOOL, WeatherForecast::thundering,
			WeatherForecast::new);
	public static final WeatherForecast UNKNOWN = new WeatherForecast(-1, false, false);
	private static final int SEND_INTERVAL = 20;

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % SEND_INTERVAL != 0) {
				return;
			}
			WeatherForecast forecast = null;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (hasClock(player) && ServerPlayNetworking.canSend(player, TYPE)) {
					if (forecast == null) {
						forecast = of(server);
					}
					ServerPlayNetworking.send(player, forecast);
				}
			}
		});
	}

	/** A clock anywhere in the inventory, hotbar and off hand included, or in the worn backpack. */
	public static boolean hasClock(Player player) {
		if (player.getInventory().contains(stack -> stack.is(Items.CLOCK))) {
			return true;
		}
		for (ItemStackTemplate stack : PlayerGear.of(player).backpack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems()) {
			if (stack.item().value() == Items.CLOCK) {
				return true;
			}
		}
		return false;
	}

	/** Follows the timers of ServerLevel.advanceWeatherCycle, without the random draws that come after them. */
	static WeatherForecast of(MinecraftServer server) {
		if (!server.getGameRules().get(GameRules.ADVANCE_WEATHER)) {
			return UNKNOWN;
		}
		WeatherData weather = server.getWeatherData();
		int clear = weather.getClearWeatherTime();
		int rain = weather.getRainTime();
		int thunder = weather.getThunderTime();
		boolean raining = weather.isRaining();
		boolean thundering = weather.isThundering();
		if (clear > 0) {
			// Clear weather set by a command: both timers are held at 1 until it ends, then rain and thunder start
			return new WeatherForecast(clear + 1, true, true);
		}
		if (raining && thunder > 0 && (rain == 0 || thunder < rain)) {
			return new WeatherForecast(thunder, true, !thundering);
		}
		if (rain > 0) {
			// Thunder only shows with rain. When it changes before the rain does, its next timer is
			// drawn at random, so it stays unknown: the forecast then says rain without thunder
			boolean thunderAfter = thunder == rain ? !thundering : thunder > rain && thundering;
			return new WeatherForecast(rain, !raining, thunderAfter);
		}
		return UNKNOWN;
	}

	@Override
	public Type<WeatherForecast> type() {
		return TYPE;
	}
}
