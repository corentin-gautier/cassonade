package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;

/**
 * The creeper, its leaves and skin, from the front, side and back by day, charged too, a row of them each with its own
 * leaves, then at night one at rest, its heart glimmering, beside one swelling, its cracks glowing.
 */
public class CreeperLookClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("fill -8 99 -8 8 99 16 grass_block");
			world.getServer().runCommand("fill -8 100 -8 8 112 16 air");
			world.getServer().runCommand("kill @e[type=!player]");
			world.getServer().runCommand("tp @a 0.5 100 1.5 0 12");

			// Turned away, side on, facing the player, and a charged one, whose swirl follows vanilla's boxes
			world.getServer().runCommand("summon creeper -2.5 100 5.5 {NoAI:1b,Rotation:[0f,0f]}");
			world.getServer().runCommand("summon creeper -0.5 100 5.5 {NoAI:1b,Rotation:[90f,0f]}");
			world.getServer().runCommand("summon creeper 1.5 100 5.5 {NoAI:1b,Rotation:[180f,0f]}");
			world.getServer().runCommand("summon creeper 3.5 100 5.5 {NoAI:1b,Rotation:[180f,0f],powered:1b}");
			context.waitTicks(20);
			Screenshots.take(context, "cassonade-creeper-day");
			// Up close, before the one facing the player: its face and its ribcage
			world.getServer().runCommand("tp @a 1.5 100 3.3 0 28");
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-creeper-close");

			// A row of them, each growing its own leaves and branches, once the last ones are done dying and their puffs of smoke gone
			world.getServer().runCommand("kill @e[type=creeper]");
			context.waitTicks(60);
			world.getServer().runCommand("kill @e[type=item]");
			for (int x = -5; x <= 6; x += 2) {
				world.getServer().runCommand("summon creeper " + (x + 0.5) + " 100 8.5 {NoAI:1b,Rotation:[180f,0f]}");
			}
			world.getServer().runCommand("tp @a 0.5 100 0.5 0 10");
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-creeper-variety");

			// A long fuse, so it is caught swelling well before it blows
			world.getServer().runCommand("kill @e[type=creeper]");
			world.getServer().runCommand("time set midnight");
			// Beside it one at rest, its heart only glimmering
			world.getServer().runCommand("summon creeper 2.0 100 4.5 {NoAI:1b,Rotation:[180f,0f],Fuse:200s,ignited:1b}");
			world.getServer().runCommand("summon creeper -1.0 100 4.5 {NoAI:1b,Rotation:[180f,0f]}");
			context.waitTicks(150);
			Screenshots.take(context, "cassonade-creeper-swelling");
			float swelling = world.getServer().computeOnServer(server ->
					server.overworld().getEntitiesOfClass(Creeper.class, new AABB(1, 98, 0, 4, 104, 8)).getFirst().getSwelling(1.0F));
			if (swelling <= 0.3F) {
				throw new AssertionError("Expected the creeper swelling, at " + swelling);
			}
			world.getServer().runCommand("kill @e[type=creeper]");
		}
	}
}
