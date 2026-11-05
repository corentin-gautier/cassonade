package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.EntityHitResult;

/** The crosshair picks a baby zombie when it passes just beside its hitbox, which stays the game's size. */
public class BabyZombiesClientGameTest implements FabricClientGameTest {
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
			world.getServer().runCommand("kill @e[type=!player]");
			// Its hitbox runs from x 0.255 to 0.745: the look passes 0.055 beside it, inside the tenth added around
			world.getServer().runCommand("summon zombie 0.5 100 5.5 {NoAI:1b,IsBaby:1b,Silent:1b}");
			world.getServer().runCommand("tp @a 0.8 100 3 0 25");
			context.waitTicks(10);

			check(context.computeOnClient(client -> {
				Zombie zombie = client.level.getEntitiesOfClass(Zombie.class, client.player.getBoundingBox().inflate(8)).getFirst();
				return zombie.isBaby() && client.player.getEyePosition().x > zombie.getBoundingBox().maxX;
			}), "the look passing beside the baby's real hitbox");
			check(context.computeOnClient(client -> client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Zombie),
					"the crosshair picking the baby zombie");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
