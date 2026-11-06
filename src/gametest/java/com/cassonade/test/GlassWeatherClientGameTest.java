package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.cassonade.client.ambience.GlassWeather;

/** Drops on glass under the rain, then frost on the same glass in a snowy biome at night, a screenshot of each. */
public class GlassWeatherClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamerule spawn_mobs false");
			// No fall when sent away and back for the new biome
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("fill -6 99 -6 6 99 6 stone");
			world.getServer().runCommand("fill -6 100 -6 6 106 6 air");
			// A wall of glass blocks and, beside it, one of panes, the rain on both sides
			world.getServer().runCommand("fill -3 100 3 -1 102 3 glass");
			world.getServer().runCommand("fill 0 100 3 0 102 3 light_blue_stained_glass");
			world.getServer().runCommand("fill 1 100 3 3 102 3 glass_pane");
			world.getServer().runCommand("tp @a 0.5 100 -0.5 0 10");
			world.getServer().runCommand("weather rain");
			context.waitFor(client -> client.level.getRainLevel(1) >= 1, 200);
			context.runOnClient(client -> GlassWeather.rescan());
			context.waitTicks(25);
			context.runOnClient(client -> check(GlassWeather.wetFaces() > 0, "wet glass under the rain: " + GlassWeather.wetFaces()));
			Screenshots.take(context, "cassonade-glass-rain");

			// In the snow at night, the rain gone: frost from the edges, and no water left
			world.getServer().runCommand("fillbiome -8 96 -8 8 110 8 minecraft:snowy_plains");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set midnight");
			// The client learns of the new biome with the chunks sent again
			world.getServer().runCommand("tp @a 200 100 200");
			context.waitTicks(40);
			world.getServer().runCommand("tp @a 0.5 100 -0.5 0 10");
			context.waitTicks(40);
			context.runOnClient(client -> GlassWeather.rescan());
			context.waitTicks(5);
			// It creeps in a pixel every few seconds: under way, then where it would have got to
			context.runOnClient(client -> check(GlassWeather.frostReach() > 0, "the frost under way"));
			context.runOnClient(client -> GlassWeather.settleFrost(client.level));
			context.runOnClient(client -> check(GlassWeather.frostFaces() > 0, "frosted glass in the snow: " + GlassWeather.frostFaces()));
			context.runOnClient(client -> check(GlassWeather.wetFaces() == 0, "no rain in a snowy biome: " + GlassWeather.wetFaces()));
			Screenshots.take(context, "cassonade-glass-frost");

			// Glass placed beside the window counts at once, not at the next look around a second later
			int before = context.computeOnClient(client -> GlassWeather.frostFaces());
			world.getServer().runCommand("setblock -4 100 3 glass");
			context.waitTicks(3);
			context.runOnClient(client -> check(GlassWeather.frostFaces() != before, "the new glass at once: " + before + " faces before, "
					+ GlassWeather.frostFaces() + " after"));
		}
	}
}
