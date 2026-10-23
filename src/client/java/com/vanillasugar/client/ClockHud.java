package com.vanillasugar.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.attribute.EnvironmentAttributes;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.map.FramedPanel;
import com.vanillasugar.client.map.SmallText;
import com.vanillasugar.clock.WeatherForecast;

/**
 * Panel shown in the top left corner while the player wears a clock in its gear slot: the time and the weather,
 * the day, and after the weather icon a small one for the next change of weather, sent by the server
 * (none on a server without the mod). A small bed after the time tells when a bed would let the player sleep.
 */
public final class ClockHud {
	private static final int MARGIN = 4;
	private static final int BORDER = FramedPanel.BORDER;
	private static final int PADDING = 3;
	private static final int ICON_SIZE = 13;
	private static final int SMALL_ICON_SIZE = 9;
	private static final int GAP = 3;
	private static final Component ARROW = Component.literal("»");
	private static final Identifier SLEEP = VanillaSugar.id("hud/sleep");
	private static final int TICKS_PER_DAY = 24000;
	/** Clear nights show the moon, from sunset to sunrise in ticks of the day. */
	private static final int NIGHT_START = 13000;
	private static final int NIGHT_END = 23000;
	/** A forecast older than this is dropped: the clock was put away, or the server stopped sending them. */
	private static final int FORECAST_LIFETIME = 60;

	private enum Weather {
		CLEAR_DAY("clear_day"),
		CLEAR_NIGHT("clear_night"),
		CLOUDY("cloudy"),
		RAIN("rain"),
		SNOW("snow"),
		THUNDER("thunder");

		final Identifier icon;
		final Identifier smallIcon;

		Weather(String name) {
			this.icon = VanillaSugar.id("hud/weather_" + name);
			this.smallIcon = VanillaSugar.id("hud/weather_" + name + "_small");
		}
	}

	private static boolean shown;
	private static WeatherForecast forecast = WeatherForecast.UNKNOWN;
	private static long forecastReceivedAt;

	private ClockHud() {
	}

	public static void tick(Minecraft minecraft) {
		shown = minecraft.player != null && WeatherForecast.hasClock(minecraft.player);
	}

	public static boolean isShown() {
		return shown;
	}

	public static void receive(Minecraft minecraft, WeatherForecast received) {
		forecast = received;
		forecastReceivedAt = minecraft.level.getGameTime();
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (!shown || level == null || minecraft.getDebugOverlay().showDebugScreen()) {
			return;
		}
		Font font = minecraft.font;
		BlockPos pos = minecraft.player.blockPosition();

		// The dimension's own clock, like the clock item: none in the Nether, where it spins
		boolean hasClock = level.dimensionType().defaultClock().isPresent();
		long time = level.getDefaultClockTime();
		Component clock = Component.literal(hasClock ? hourOf(time) : "--:--");
		Component day = Component.translatable("gui.vanillasugar.clock.day", level.getOverworldClockTime() / TICKS_PER_DAY + 1);
		Weather weather = level.canHaveWeather() ? current(level, pos, time) : null;
		Weather next = weather != null && hasClock ? forecast(level, pos, time) : null;
		// The bed's own rule where the player stands: at night or in a thunderstorm, never in the Nether or the End
		boolean canSleep = level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos).canSleep(level);
		int clockWidth = font.width(clock) + (canSleep ? 2 + SMALL_ICON_SIZE : 0);

		int smallLine = SmallText.lineHeight(font);
		// Current weather >> forecast, on one line before the text
		int arrowWidth = SmallText.width(font, ARROW);
		int iconsWidth = weather == null ? 0 : next == null ? ICON_SIZE : ICON_SIZE + 2 + arrowWidth + 2 + SMALL_ICON_SIZE;
		int textLeft = weather != null ? iconsWidth + GAP + 1 : 0;
		int textHeight = font.lineHeight + 1 + smallLine;
		int contentHeight = Math.max(weather != null ? ICON_SIZE : 0, textHeight);
		int contentWidth = textLeft + Math.max(clockWidth, SmallText.width(font, day));
		int width = 2 * (BORDER + PADDING) + contentWidth;
		int height = 2 * (BORDER + PADDING) + contentHeight;

		int x = MARGIN + BORDER + PADDING;
		int y = MARGIN + BORDER + PADDING;
		FramedPanel.draw(graphics, MARGIN, MARGIN, width, height);

		if (weather != null) {
			int iconTop = y + (contentHeight - ICON_SIZE) / 2;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, weather.icon, x, iconTop, ICON_SIZE, ICON_SIZE);
			if (next != null) {
				int arrowX = x + ICON_SIZE + 2;
				SmallText.left(graphics, font, ARROW, arrowX, iconTop + (ICON_SIZE - smallLine) / 2f, 0xFFB0B0B0);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, next.smallIcon, arrowX + arrowWidth + 2, iconTop + (ICON_SIZE - SMALL_ICON_SIZE) / 2,
						SMALL_ICON_SIZE, SMALL_ICON_SIZE);
			}
		}
		int textTop = y + (contentHeight - textHeight) / 2;
		graphics.text(font, clock, x + textLeft, textTop, 0xFFFFFFFF, true);
		if (canSleep) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLEEP, x + textLeft + font.width(clock) + 2, textTop + (font.lineHeight - 1 - SMALL_ICON_SIZE) / 2,
					SMALL_ICON_SIZE, SMALL_ICON_SIZE);
		}
		SmallText.left(graphics, font, day, x + textLeft, textTop + font.lineHeight + 1, 0xFFD0D0D0);
	}

	private static Weather current(ClientLevel level, BlockPos pos, long time) {
		return weather(level, pos, level.isRaining(), level.isThundering(), time);
	}

	/** Snow or rain after the biome the player stands in; neither where it never falls, as in deserts. */
	private static Weather weather(ClientLevel level, BlockPos pos, boolean raining, boolean thundering, long time) {
		if (thundering && raining) {
			return Weather.THUNDER;
		}
		if (raining) {
			return switch (level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel())) {
				case RAIN -> Weather.RAIN;
				case SNOW -> Weather.SNOW;
				case NONE -> Weather.CLOUDY;
			};
		}
		long tickOfDay = Math.floorMod(time, TICKS_PER_DAY);
		return tickOfDay >= NIGHT_START && tickOfDay < NIGHT_END ? Weather.CLEAR_NIGHT : Weather.CLEAR_DAY;
	}

	/** Ticks left before the forecast change, counted down between two messages; -1 when unknown. */
	private static int forecastTicks(ClientLevel level) {
		long age = level.getGameTime() - forecastReceivedAt;
		if (forecast.ticks() < 0 || age < 0 || age > FORECAST_LIFETIME) {
			return -1;
		}
		return (int) Math.max(0, forecast.ticks() - age);
	}

	/** The weather the next change brings, as it will look then; null when unknown. */
	private static Weather forecast(ClientLevel level, BlockPos pos, long time) {
		int ticks = forecastTicks(level);
		return ticks < 0 ? null : weather(level, pos, forecast.raining(), forecast.thundering(), time + ticks);
	}

	/** The time of day as on a watch, 24 hours: tick 0 of the day is 6:00. */
	private static String hourOf(long time) {
		long tickOfDay = Math.floorMod(time, TICKS_PER_DAY);
		long hours = (tickOfDay / 1000 + 6) % 24;
		long minutes = tickOfDay % 1000 * 60 / 1000;
		return String.format("%02d:%02d", hours, minutes);
	}
}
