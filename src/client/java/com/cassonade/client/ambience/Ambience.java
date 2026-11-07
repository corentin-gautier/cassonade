package com.cassonade.client.ambience;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

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
			Birds.tick(minecraft);
			Murmuration.tick(minecraft);
			Butterflies.tick(minecraft);
			Robins.tick(minecraft);
			Squirrels.tick(minecraft);
			if (level != null && minecraft.player != null && !minecraft.isPaused()) {
				Motes.tick(minecraft, level);
				AirSounds.tick(minecraft, level);
				Waves.tick(minecraft, level);
				JumpingFish.tick(minecraft, level);
				Plankton.tick(minecraft, level);
				Breath.tick(minecraft, level);
				Splashes.tick(minecraft, level);
				AfterRain.tick(level);
				GlassWeather.tick(minecraft, level);
			}
			MorningMist.tick(minecraft);
			Storm.tick(minecraft);
			Aurora.tick(minecraft);
			ShootingStars.tick(minecraft);
			HeatLightning.tick(minecraft);
		});
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			Birds.draw(context);
			Murmuration.draw(context);
			Butterflies.draw(context);
			Robins.draw(context);
			Squirrels.draw(context);
			Aurora.draw(context);
			ShootingStars.draw(context);
			HeatLightning.draw(context);
			JumpingFish.draw(context);
			Foam.draw(context);
			Plankton.draw(context);
			GlassWeather.draw(context);
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
