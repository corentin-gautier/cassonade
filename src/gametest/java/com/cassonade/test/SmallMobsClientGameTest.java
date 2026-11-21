package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.EntityHitResult;

/** The crosshair picks a baby zombie, a silverfish or an endermite when it passes just beside its hitbox, which stays the game's size. */
public class SmallMobsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("fill -4 99 -4 4 99 10 stone");
			world.getServer().runCommand("fill -4 100 -4 4 104 10 air");
			// A baby zombie's hitbox runs from x 0.255 to 0.745, a silverfish's and an endermite's from 0.3 to 0.7: each
			// look passes 0.05 beside it, inside the tenth added around, low enough to meet the small ones
			checkPicked(context, world, "zombie", "{IsBaby:1b}", 0.8, 25);
			checkPicked(context, world, "silverfish", "", 0.75, 30);
			checkPicked(context, world, "endermite", "", 0.75, 30);
		}
	}

	private static void checkPicked(ClientGameTestContext context, TestSingleplayerContext world, String type, String data, double x, int pitch) {
		world.getServer().runCommand("kill @e[type=!player]");
		String tag = data.isEmpty() ? "{NoAI:1b,Silent:1b}" : "{NoAI:1b,Silent:1b," + data.substring(1);
		world.getServer().runCommand("summon " + type + " 0.5 100 5.5 " + tag);
		world.getServer().runCommand("tp @a " + x + " 100 3 0 " + pitch);
		context.waitTicks(10);

		check(context.computeOnClient(client -> {
			Mob mob = client.level.getEntitiesOfClass(Mob.class, client.player.getBoundingBox().inflate(8)).getFirst();
			return client.player.getEyePosition().x > mob.getBoundingBox().maxX;
		}), "the look passing beside the " + type + "'s real hitbox");
		check(context.computeOnClient(client -> client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Mob),
				"the crosshair picking the " + type);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
