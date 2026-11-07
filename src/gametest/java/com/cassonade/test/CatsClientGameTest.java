package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.AABB;

/** A spider runs from a cat close by; a cat goes for a spider it sees and kills it, the spider never turning on it; a cave spider too. */
public class CatsClientGameTest implements FabricClientGameTest {
	private static final AABB AREA = new AABB(-20, 90, -20, 20, 120, 40);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamerule spawn_mobs false");
			// A stray cat runs from a player it sees.
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("time set midnight");
			world.getServer().runCommand("fill -8 99 -8 8 99 30 stone");
			world.getServer().runCommand("fill -8 100 -8 8 110 30 air");
			world.getServer().runCommand("kill @e[type=!player]");
			world.getServer().runCommand("tp @a 0.5 100 -6.5 0 0");

			// A spider runs from a cat close by, even one that doesn't move
			world.getServer().runCommand("summon cat 0.5 100 4.5 {NoAI:1b}");
			world.getServer().runCommand("summon spider 0.5 100 6.5");
			context.waitTicks(100);
			check(world.getServer().computeOnServer(server -> {
				Spider spider = server.overworld().getEntitiesOfClass(Spider.class, AREA).getFirst();
				return spider.distanceTo(server.overworld().getEntitiesOfClass(Cat.class, AREA).getFirst()) > 5;
			}), "a spider running from a cat 2 blocks away");
			world.getServer().runCommand("kill @e[type=!player]");

			for (String spider : new String[] { "spider", "cave_spider" }) {
				world.getServer().runCommand("summon cat 0.5 100 4.5");
				world.getServer().runCommand("summon " + spider + " 0.5 100 12.5");
				boolean dead = false;
				for (int tick = 0; tick < 600 && !dead; tick++) {
					context.waitTick();
					dead = world.getServer().computeOnServer(server -> server.overworld().getEntitiesOfClass(Spider.class, AREA).isEmpty());
				}
				check(dead, "a cat killing the " + spider + " it saw");
				check(world.getServer().computeOnServer(server -> {
					Cat cat = server.overworld().getEntitiesOfClass(Cat.class, AREA).getFirst();
					return cat.getHealth() == cat.getMaxHealth();
				}), "the " + spider + " leaving the cat unhurt");
				world.getServer().runCommand("kill @e[type=cat]");
			}
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
