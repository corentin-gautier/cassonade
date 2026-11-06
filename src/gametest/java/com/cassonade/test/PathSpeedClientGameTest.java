package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;
import static com.cassonade.test.PlannedWorld.player;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;

import com.cassonade.Cassonade;

/** Faster on a dirt path than on stone, the same walk timed on both; a ridden horse too, a loose one not. */
public class PathSpeedClientGameTest implements FabricClientGameTest {
	private static final Identifier BONUS = Cassonade.id("path_speed");
	private static final int WALK = 40;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("fill -2 99 -4 60 99 8 stone");
			world.getServer().runCommand("fill -2 100 -4 60 104 8 air");
			world.getServer().runCommand("fill -2 99 4 60 99 4 dirt_path");

			double onStone = walk(context, world, 0);
			double onPath = walk(context, world, 4);
			double ratio = onPath / onStone;
			check(ratio > 1.1 && ratio < 1.2, "about 15% farther on the path: " + onStone + " on stone, " + onPath + " on the path");
			System.out.println("Path speed: " + onStone + " blocks on stone, " + onPath + " on the path");

			// The client is told, as it moves the player itself
			world.getServer().runCommand("tp @a 2.5 100 4.5");
			context.waitTicks(5);
			context.runOnClient(client -> check(client.player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(BONUS), "the bonus on the client"));
			world.getServer().runCommand("tp @a 2.5 100 0.5");
			context.waitTicks(5);
			context.runOnClient(client -> check(!client.player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(BONUS), "no bonus off the path"));

			// A horse: only ridden (a mob without AI has no rider in control, so this one has its AI)
			world.getServer().runCommand("summon horse 10.5 100 4.5 {Tame:1b,equipment:{saddle:{id:\"minecraft:saddle\"}}}");
			context.waitTicks(5);
			check(!horseHasBonus(world), "no bonus for a horse on its own");
			world.getServer().runCommand("ride @p mount @e[type=horse,limit=1]");
			context.waitTicks(5);
			check(horseHasBonus(world), "the bonus for a ridden horse: " + world.getServer().computeOnServer(server -> {
				Horse horse = horse(server);
				return "saddled " + horse.isSaddled() + ", rider " + horse.getFirstPassenger() + ", in control " + horse.getControllingPassenger()
						+ ", on " + server.overworld().getBlockState(horse.getBlockPosBelowThatAffectsMyMovement()) + " at " + horse.position();
			}));
		}
	}

	/** Blocks walked east in a fixed time along the row at that z, from a standstill. */
	private static double walk(ClientGameTestContext context, TestSingleplayerContext world, int z) {
		world.getServer().runCommand("tp @a 0.5 100 " + (z + 0.5) + " -90 0");
		context.waitTicks(10);
		double start = world.getServer().computeOnServer(server -> player(server).getX());
		context.getInput().holdKeyFor(options -> options.keyUp, WALK);
		context.waitTicks(2);
		return world.getServer().computeOnServer(server -> player(server).getX()) - start;
	}

	private static boolean horseHasBonus(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> horse(server).getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(BONUS));
	}

	private static Horse horse(MinecraftServer server) {
		return server.overworld().getEntitiesOfClass(Horse.class, player(server).getBoundingBox().inflate(20)).getFirst();
	}
}
