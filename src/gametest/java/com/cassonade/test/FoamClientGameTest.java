package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Foam;
import com.cassonade.client.ambience.Waves;

/** A line of foam and splashes along the sea's edge, white by day and moonlit blue at night, gone once the wave is over; none by a lake. */
public class FoamClientGameTest implements FabricClientGameTest {
	/** Half of a surge, when it is at its fullest. */
	private static final int TO_FULLEST = 35;
	private static final int LIFE = 70;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				Birds.setSpawning(false);
				Waves.setSurging(false);
			});
			world.getServer().runCommand("gamerule send_command_feedback false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("execute as @p at @s run fill ~-12 ~-1 ~-16 ~7 ~-1 ~16 minecraft:sand");
			world.getServer().runCommand("execute as @p at @s run fill ~8 ~-3 ~-16 ~24 ~-1 ~16 minecraft:water");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			context.runOnClient(client -> Foam.surge(client, client.level));
			check(context.computeOnClient(client -> Foam.patches() + Foam.splashes()) == 0, "no foam by a lake");

			world.getServer().runCommand("execute as @p at @s run fillbiome ~6 -64 ~-16 ~24 -40 ~16 minecraft:ocean");
			context.waitTicks(20);
			// A single block of water in the sand, as a field's, takes no foam, the shore next to it still does
			world.getServer().runCommand("execute as @p at @s run setblock ~6 ~-1 ~-12 minecraft:water");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(3);
			context.runOnClient(client -> Foam.surge(client, client.level));
			check(context.computeOnClient(client -> Foam.patchesAround(client.player.blockPosition().offset(6, -1, -12))) == 0, "no foam on a single block of water");
			check(context.computeOnClient(client -> Foam.patches()) > 20, "foam along the shore by it");
			context.waitFor(client -> Foam.patches() == 0 && Foam.splashes() == 0, LIFE);
			world.getServer().runCommand("execute as @p at @s run setblock ~6 ~-1 ~-12 minecraft:sand");
			// Hovering over the sand, looking down at the edge
			world.getServer().runCommand("execute as @p at @s run tp @a ~2 ~4 ~ -90 30");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(3);
			Screenshots.take(context, "cassonade-foam-none");
			surge(context, "cassonade-foam-day");
			world.getServer().runCommand("time set 18000");
			context.waitTicks(LIFE);
			surge(context, "cassonade-foam-night");
			context.waitFor(client -> Foam.patches() == 0 && Foam.splashes() == 0, LIFE);
		} finally {
			context.runOnClient(client -> Waves.setSurging(true));
		}
	}

	private static void surge(ClientGameTestContext context, String shot) {
		context.runOnClient(client -> Foam.surge(client, client.level));
		check(context.computeOnClient(client -> Foam.patches()) > 20, "foam along the edge");
		check(context.computeOnClient(client -> Foam.splashes()) > 0, "splashes along the edge");
		context.waitTicks(TO_FULLEST);
		Screenshots.take(context, shot);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
