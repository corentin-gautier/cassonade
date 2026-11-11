package com.cassonade.client.ambience;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * What makes the Overworld feel inhabited: nothing but pictures and sounds on the client, so it works on any server and
 * changes nothing in the game. No entity is added, and nothing can be touched.
 */
public final class Ambience {
	public static final int TICKS_PER_DAY = 24000;
	private static final int DAWN = 23500;
	private static final int SUNSET = 12000;

	private Ambience() {
	}

	public static void init() {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			AmbienceCommands.register();
		}
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			ClientLevel level = minecraft.level;
			// Each one switched off stops where it is, and goes on from there once on again
			if (Settings.on(Setting.FLOCKS)) {
				Birds.tick(minecraft);
				Murmuration.tick(minecraft);
			}
			if (Settings.on(Setting.BUTTERFLIES)) {
				Butterflies.tick(minecraft);
			}
			if (Settings.on(Setting.SONGBIRDS)) {
				Robins.tick(minecraft);
				Sparrows.tick(minecraft);
				Swallows.tick(minecraft);
			}
			if (Settings.on(Setting.SQUIRRELS)) {
				Squirrels.tick(minecraft);
			}
			if (Settings.on(Setting.SCORPIONS)) {
				Scorpions.tick(minecraft);
			}
			if (level != null && minecraft.player != null && !minecraft.isPaused()) {
				if (Settings.on(Setting.FLOATING_PARTICLES)) {
					Motes.tick(minecraft, level);
					Breath.tick(minecraft, level);
				}
				if (Settings.on(Setting.WILDLIFE_SOUNDS)) {
					AirSounds.tick(minecraft, level);
				}
				// Always: its sound fades out on its own when switched off
				Waves.tick(minecraft, level);
				if (Settings.on(Setting.SEA_LIFE)) {
					JumpingFish.tick(minecraft, level);
					Plankton.tick(minecraft, level);
				}
				if (Settings.on(Setting.WATER_SPLASHES)) {
					Splashes.tick(minecraft, level);
				}
				if (Settings.on(Setting.RAIN_ON_GLASS)) {
					AfterRain.tick(level);
					GlassWeather.tick(minecraft, level);
				}
			}
			MorningMist.tick(minecraft);
			Storm.tick(minecraft);
			if (Settings.on(Setting.NIGHT_SKY)) {
				Aurora.tick(minecraft);
				ShootingStars.tick(minecraft);
			}
			if (Settings.on(Setting.HEAT_LIGHTNING)) {
				HeatLightning.tick(minecraft);
			}
		});
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			if (Settings.on(Setting.FLOCKS)) {
				Birds.draw(context);
				Murmuration.draw(context);
			}
			if (Settings.on(Setting.BUTTERFLIES)) {
				Butterflies.draw(context);
			}
			if (Settings.on(Setting.SONGBIRDS)) {
				Robins.draw(context);
				Sparrows.draw(context);
				Swallows.draw(context);
			}
			if (Settings.on(Setting.SQUIRRELS)) {
				Squirrels.draw(context);
			}
			if (Settings.on(Setting.SCORPIONS)) {
				Scorpions.draw(context);
			}
			if (Settings.on(Setting.NIGHT_SKY)) {
				Aurora.draw(context);
				ShootingStars.draw(context);
			}
			if (Settings.on(Setting.HEAT_LIGHTNING)) {
				HeatLightning.draw(context);
			}
			if (Settings.on(Setting.SEA_LIFE)) {
				JumpingFish.draw(context);
				Plankton.draw(context);
			}
			if (Settings.on(Setting.WAVES)) {
				Foam.draw(context);
			}
			if (Settings.on(Setting.RAIN_ON_GLASS)) {
				GlassWeather.draw(context);
			}
		});
	}

	static boolean overworld(ClientLevel level) {
		return level.dimension() == Level.OVERWORLD;
	}

	/** The time of day, 0 at sunrise, as the clock item reads it. */
	static long tickOfDay(ClientLevel level) {
		return Math.floorMod(level.getDefaultClockTime(), TICKS_PER_DAY);
	}

	/** From just before sunrise until sunset: stricter than the sky's brightness, still high well into the dusk. */
	static boolean daytime(ClientLevel level) {
		long tick = tickOfDay(level);
		return tick >= DAWN || tick < SUNSET;
	}
}
