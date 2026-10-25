package com.cassonade.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.WorldClocks;

import com.cassonade.Cassonade;
import com.cassonade.client.map.FramedPanel;
import com.cassonade.client.map.SmallText;
import com.cassonade.clock.WeatherForecast;
import com.cassonade.time.LongDay;

/**
 * Panel shown in the top left corner while the player wears a clock in its gear slot, in parts, thin lines between
 * those after the time: the weather, and with a barometer in the slot instead a small icon for its next change, sent by the server
 * (none on a server without the mod); the time over the day; and the moon's phase, where there is weather. Once a
 * bed would let the player sleep, a soft chime rings and the moon blinks with a small bed for a few seconds, so the
 * panel keeps its width.
 */
public final class ClockHud {
	private static final int MARGIN = 4;
	private static final int BORDER = FramedPanel.BORDER;
	private static final int PADDING = 3;
	private static final int ICON_SIZE = 13;
	private static final int SMALL_ICON_SIZE = 9;
	/** Between the weather and the time. */
	private static final int GAP = 4;
	/** Between a part of the panel and the line that sets it apart from the next. */
	private static final int SEPARATOR_GAP = 3;
	/** The theme's WOOD (tools/gui_theme.py), the panel's frame showing through. */
	private static final int SEPARATOR_COLOR = 0xFF6E4838;
	private static final Component ARROW = Component.literal("»");
	private static final Identifier SLEEP = Cassonade.id("hud/sleep");
	private static final int TICKS_PER_DAY = 24000;
	/** Clear nights show the moon, from sunset to sunrise in ticks of the day. */
	private static final int NIGHT_START = 13000;
	private static final int NIGHT_END = 23000;
	/** How long the moon blinks with the bed once one can sleep, and how long each shows. */
	private static final int BLINK = 100;
	private static final int BLINK_STEP = 10;
	/** A forecast older than this is dropped: the barometer was put away, or the server stopped sending them. */
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
			this.icon = Cassonade.id("hud/weather_" + name);
			this.smallIcon = Cassonade.id("hud/weather_" + name + "_small");
		}
	}

	private static boolean shown;
	/** Whether a bed would let the player sleep at the last tick; null until known, so joining at night stays quiet. */
	private static Boolean couldSleep;
	/** Ticks left of the moon blinking with the bed. */
	private static int blinking;
	/** Where the panel drawn last stops on the right, 0 when it is not drawn. */
	private static int right;
	private static WeatherForecast forecast = WeatherForecast.UNKNOWN;
	private static long forecastReceivedAt;

	private ClockHud() {
	}

	public static void tick(Minecraft minecraft) {
		shown = minecraft.player != null && WeatherForecast.hasClock(minecraft.player);
		if (!shown || minecraft.level == null) {
			couldSleep = null;
			blinking = 0;
			return;
		}
		boolean canSleep = canSleep(minecraft.level, minecraft.player.blockPosition());
		if (canSleep && Boolean.FALSE.equals(couldSleep)) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F, 0.4F));
			blinking = BLINK;
		} else if (blinking > 0) {
			blinking--;
		}
		couldSleep = canSleep;
	}

	/** The bed's own rule where the player stands: at night or in a thunderstorm, never in the Nether or the End. */
	private static boolean canSleep(ClientLevel level, BlockPos pos) {
		return level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos).canSleep(level);
	}

	/** Whether the moon's place shows the bed right now, while it blinks: for the tests. */
	public static boolean showsBed() {
		return blinking > 0 && (BLINK - blinking) / BLINK_STEP % 2 == 0;
	}

	public static boolean isShown() {
		return shown;
	}

	public static void receive(Minecraft minecraft, WeatherForecast received) {
		forecast = received;
		forecastReceivedAt = minecraft.level.getGameTime();
	}

	/** Where the panel stops on the right, so what sits next to it in the top left corner follows; 0 without it. */
	public static int right() {
		return right;
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		right = 0;
		if (!shown || level == null || minecraft.getDebugOverlay().showDebugScreen()) {
			return;
		}
		Font font = minecraft.font;
		BlockPos pos = minecraft.player.blockPosition();

		// The dimension's own clock, like the clock item: none in the Nether, where it spins
		boolean hasClock = level.dimensionType().defaultClock().isPresent();
		long time = level.getDefaultClockTime();
		Component clock = Component.literal(hasClock ? hourOf(time) : "--:--");
		Component day = Component.translatable("gui.cassonade.clock.day", level.getOverworldClockTime() / TICKS_PER_DAY + 1);
		Weather weather = level.canHaveWeather() ? current(level, pos, time) : null;
		Weather next = weather != null && hasClock && WeatherForecast.hasBarometer(minecraft.player) ? forecast(level, pos, time) : null;
		// The moon where there is weather, so a sky: in the overworld
		Identifier moon = weather != null && hasClock
				? Cassonade.id("hud/moon_" + level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos).getSerializedName()) : null;

		int smallLine = SmallText.lineHeight(font);
		int textHeight = font.lineHeight + 1 + smallLine;
		int contentHeight = Math.max(weather != null ? ICON_SIZE : 0, textHeight);
		int y = MARGIN + BORDER + PADDING;

		// Side by side: weather » forecast, time over day | moon, or the bed while it blinks
		List<Section> sections = new ArrayList<>();
		if (weather != null) {
			int arrowWidth = SmallText.width(font, ARROW);
			sections.add(new Section(next == null ? ICON_SIZE : ICON_SIZE + 2 + arrowWidth + 2 + SMALL_ICON_SIZE, false, x -> {
				int iconTop = y + (contentHeight - ICON_SIZE) / 2;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, weather.icon, x, iconTop, ICON_SIZE, ICON_SIZE);
				if (next != null) {
					int arrowX = x + ICON_SIZE + 2;
					SmallText.left(graphics, font, ARROW, arrowX, iconTop + (ICON_SIZE - smallLine) / 2f, 0xFFB0B0B0);
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, next.smallIcon, arrowX + arrowWidth + 2, iconTop + (ICON_SIZE - SMALL_ICON_SIZE) / 2,
							SMALL_ICON_SIZE, SMALL_ICON_SIZE);
				}
			}));
		}
		// The time reads with the weather, no line between them
		sections.add(new Section(Math.max(font.width(clock), SmallText.width(font, day)), false, x -> {
			int textTop = y + (contentHeight - textHeight) / 2;
			graphics.text(font, clock, x, textTop, 0xFFFFFFFF, true);
			SmallText.left(graphics, font, day, x, textTop + font.lineHeight + 1, 0xFFD0D0D0);
		}));
		Identifier icon = showsBed() || blinking > 0 && moon == null ? SLEEP : moon;
		if (icon != null) {
			sections.add(new Section(SMALL_ICON_SIZE, true, x -> graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x,
					y + (contentHeight - SMALL_ICON_SIZE) / 2, SMALL_ICON_SIZE, SMALL_ICON_SIZE)));
		}

		int contentWidth = 0;
		for (int i = 0; i < sections.size(); i++) {
			contentWidth += (i == 0 ? 0 : sections.get(i).separated() ? 2 * SEPARATOR_GAP + 1 : GAP) + sections.get(i).width();
		}
		int width = 2 * (BORDER + PADDING) + contentWidth;
		int height = 2 * (BORDER + PADDING) + contentHeight;
		FramedPanel.draw(graphics, MARGIN, MARGIN, width, height);
		right = MARGIN + width;

		int x = MARGIN + BORDER + PADDING;
		for (int i = 0; i < sections.size(); i++) {
			if (i > 0 && sections.get(i).separated()) {
				x += SEPARATOR_GAP;
				graphics.fill(x, y + 1, x + 1, y + contentHeight - 1, SEPARATOR_COLOR);
				x += 1 + SEPARATOR_GAP;
			} else if (i > 0) {
				x += GAP;
			}
			sections.get(i).draw().accept(x);
			x += sections.get(i).width();
		}
	}

	/** A part of the panel: its width, whether a line sets it apart from the one before, and what draws it from its left edge. */
	private record Section(int width, boolean separated, IntConsumer draw) {
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
		return ticks < 0 ? null : weather(level, pos, forecast.raining(), forecast.thundering(), timeAfter(level, time, ticks));
	}

	/**
	 * The clock time {@code ticks} from now, at the rate the server sent. Only a server with the mod sends a forecast,
	 * so the overworld day runs at half pace ({@link LongDay}); the rate sent is already halved during it.
	 */
	private static long timeAfter(ClientLevel level, long time, int ticks) {
		return level.dimensionType().defaultClock().map(clock -> {
			float rate = level.clockManager().getInstance(clock).rate();
			if (!clock.is(WorldClocks.OVERWORLD)) {
				return time + (long) (ticks * rate);
			}
			return LongDay.after(time, ticks, LongDay.isDaytime(time) ? rate / LongDay.DAY_RATE : rate);
		}).orElse(time);
	}

	/** The time of day as on a watch, 24 hours: tick 0 of the day is 6:00. */
	private static String hourOf(long time) {
		long tickOfDay = Math.floorMod(time, TICKS_PER_DAY);
		long hours = (tickOfDay / 1000 + 6) % 24;
		long minutes = tickOfDay % 1000 * 60 / 1000;
		return String.format("%02d:%02d", hours, minutes);
	}
}
