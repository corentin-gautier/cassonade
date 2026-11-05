package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.phys.AABB;

/** A blaze leaves a player alone at 36 blocks, where vanilla shoots from, and goes for them at 20. */
public class BlazesClientGameTest implements FabricClientGameTest {
	private static final AABB AREA = new AABB(-20, 90, -20, 20, 120, 60);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("effect give @a resistance infinite 255 true");
			world.getServer().runCommand("effect give @a fire_resistance infinite 0 true");
			world.getServer().runCommand("fill -8 99 -8 8 99 50 stone");
			world.getServer().runCommand("fill -8 100 -8 8 110 50 air");
			world.getServer().runCommand("kill @e[type=!player]");
			world.getServer().runCommand("tp @a 0.5 100 0.5 0 0");

			world.getServer().runCommand("summon blaze 0.5 100 36.5");
			context.waitTicks(100);
			check(!world.getServer().computeOnServer(server -> targets(server.overworld().getEntitiesOfClass(Blaze.class, AREA).getFirst())),
					"a blaze 36 blocks away leaving the player alone");

			world.getServer().runCommand("kill @e[type=blaze]");
			world.getServer().runCommand("summon blaze 0.5 100 20.5");
			boolean found = false;
			for (int tick = 0; tick < 100 && !found; tick++) {
				context.waitTick();
				found = world.getServer().computeOnServer(server -> targets(server.overworld().getEntitiesOfClass(Blaze.class, AREA).getFirst()));
			}
			check(found, "a blaze 20 blocks away going for the player");
		}
	}

	private static boolean targets(Blaze blaze) {
		return blaze.getTarget() != null;
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
