package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.level.levelgen.Heightmap;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Splashes;

/** Falling water splashes where it lands, on the ground and in a pool, but not where it only runs down a step. */
public class SplashesClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 6000");
			int ground = world.getServer().computeOnServer(server -> server.overworld().getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0));
			// A fall onto the grass, from a source hanging 8 blocks up
			world.getServer().runCommand("setblock 4 " + (ground + 8) + " 0 water");
			// A step: water spreading off a block one high
			world.getServer().runCommand("setblock 12 " + ground + " 0 stone");
			world.getServer().runCommand("setblock 12 " + (ground + 1) + " 0 water");
			// A fall into a pool
			world.getServer().runCommand("fill 18 " + (ground - 3) + " -2 22 " + (ground - 1) + " 2 water");
			world.getServer().runCommand("setblock 20 " + (ground + 6) + " 0 water");
			world.getServer().runCommand("tp @a 10.5 " + (ground + 4) + " 14.5 facing 12 " + (ground + 2) + " 0");
			world.getConnection().waitForChunksRender();

			context.waitFor(client -> landing(4) != null && landing(20) != null, 20 * 10);
			// Until the water has run all the way down and spread, and every row was looked at again
			context.waitTicks(80);
			Splashes.Landing fall = landing(4);
			Splashes.Landing pool = landing(20);
			System.out.println("Splashes: " + Splashes.landings() + ", ground " + ground);
			check(fall.drop() >= 6 && fall.y() >= ground && fall.y() < ground + 1, "a fall of 7 onto the grass, found " + fall);
			check(pool.drop() >= 5 && pool.y() > ground - 1 && pool.y() <= ground, "a fall of 6 into the pool, at its surface, found " + pool);
			check(Splashes.landings().stream().allMatch(landing -> landing.y() < ground + 1), "every splash on the ground or the pool, found " + Splashes.landings());
			check(Splashes.landings().stream().noneMatch(landing -> landing.x() >= 11 && landing.x() <= 14), "no splash below a step, found " + Splashes.landings());
			world.getServer().runCommand("tp @a 9.5 " + (ground + 2) + " 6.5 facing 4 " + (ground + 1) + " 0");
			context.waitTicks(20);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			Screenshots.take(context, "cassonade-splashes");
		}
	}

	private static Splashes.Landing landing(int x) {
		List<Splashes.Landing> landings = Splashes.landings();
		return landings.stream().filter(landing -> landing.x() == x && landing.z() == 0).findFirst().orElse(null);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
