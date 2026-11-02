package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.HeatLightning;
import com.cassonade.client.ambience.JumpingFish;
import com.cassonade.client.ambience.Murmuration;
import com.cassonade.client.ambience.ShootingStars;

/** The development command calls each effect at once, by day and whatever the weather. */
public class AmbienceCommandsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("time set 6000");
			command(context, "murmuration");
			check(context.computeOnClient(client -> Murmuration.isGathered()), "murmuration");
			command(context, "geese");
			check(context.computeOnClient(client -> Birds.flockCount()) == 1, "geese");
			command(context, "crows");
			check(context.computeOnClient(client -> Birds.flockCount()) == 2, "crows");
			command(context, "shootingstar");
			check(context.computeOnClient(client -> ShootingStars.isFalling()), "shootingstar");
			command(context, "lightning");
			check(context.computeOnClient(client -> HeatLightning.isFlashing()), "lightning");
			command(context, "frogs");
			command(context, "breath");
			command(context, "fish");
			check(!context.computeOnClient(client -> JumpingFish.isLeaping()), "no fish on dry land");
			world.getServer().runCommand("execute as @p at @s run fill ~-24 ~-3 ~-24 ~24 ~-1 ~24 minecraft:water");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			command(context, "fish");
			check(context.computeOnClient(client -> JumpingFish.isLeaping()), "fish");
		}
	}

	private static void command(ClientGameTestContext context, String effect) {
		context.runOnClient(client -> client.player.connection.sendCommand("ambience " + effect));
		context.waitTicks(1);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
