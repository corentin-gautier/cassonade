package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.spider.Spider;

import com.cassonade.client.ClimbingSpiders;

/** A spider climbing a wall turns upright against it, belly to the stone, and lies flat again on the ground. */
public class SpidersClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamerule max_block_modifications 1000000");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("weather clear");
			// At night, when a spider hunts: up a wall towards a golem on its top
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("effect give @a night_vision infinite 0 true");
			world.getServer().runCommand("fill -12 99 -12 22 99 24 stone");
			world.getServer().runCommand("fill -12 100 -12 22 112 24 air");
			world.getServer().runCommand("fill 0 100 10 10 105 12 stone");
			world.getServer().runCommand("kill @e[type=!player]");
			world.getServer().runCommand("summon iron_golem 5 106 11.5 {NoAI:1b}");
			world.getServer().runCommand("tp @a 15 103 6.5 90 5");
			world.getServer().runCommand("summon spider 5 100 2");
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> client.gui.hud.toggle());

			check(context.computeOnClient(client -> climb(client) == 0), "flat on the ground before climbing");
			context.waitFor(client -> climb(client) == 1 && spider(client).getY() > 101.5, 20 * 20);
			check(context.computeOnClient(client -> spider(client).walkAnimation.speed() > 0.1F), "legs walking up the wall");
			Screenshots.take(context, "spider-climbing-side");
			// From below, while it is still on its way up
			world.getServer().runCommand("tp @a 9 101 1 35 -20");
			context.waitTicks(2);
			Screenshots.take(context, "spider-climbing-front");
			world.getServer().runCommand("kill @e[type=iron_golem]");
			world.getServer().runCommand("fill 0 100 10 10 105 12 air");
			context.waitFor(client -> climb(client) == 0 && spider(client).onGround(), 20 * 20);

			// A spider jockey: the skeleton on the back of the smaller spider, not floating above it
			world.getServer().runCommand("kill @e[type=!player]");
			world.getServer().runCommand("summon spider 5 100 4 {NoAI:1b,Rotation:[90f,0f],Passengers:[{id:\"skeleton\",NoAI:1b}]}");
			world.getServer().runCommand("tp @a 8.5 101 4 90 15");
			context.waitTicks(20);
			Screenshots.take(context, "spider-jockey");
		}
	}

	private static Spider spider(Minecraft client) {
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity instanceof Spider spider) {
				return spider;
			}
		}
		throw new AssertionError("Expected: a spider on the client");
	}

	private static float climb(Minecraft client) {
		return ((ClimbingSpiders.Climber) spider(client)).cassonade$climb(1);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
