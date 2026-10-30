package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class VehicleExitClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("fill 0 100 0 8 100 8 stone");
			world.getServer().runCommand("fill 0 101 0 8 104 8 air");
			for (String vehicle : new String[] {"minecart", "oak_boat"}) {
				// Looking east, with room everywhere: east it is, and not on the vehicle
				Vec3 exit = ride(context, world, vehicle, -90);
				check(exit.x > 4.5 + 0.5 && Math.abs(exit.z - 4.5) < 0.1 && exit.y == 101, vehicle + ": off on the side looked at (" + exit + ")");

				// A wall on that side: the next closest side, never inside the wall
				world.getServer().runCommand("fill 5 101 0 8 104 8 stone");
				exit = ride(context, world, vehicle, -90);
				check(exit.x < 5 - 0.3 + 0.01 && Math.abs(exit.x - 4.5) + Math.abs(exit.z - 4.5) > 0.5, vehicle + ": beside the wall (" + exit + ")");
				world.getServer().runCommand("fill 5 101 0 8 104 8 air");

				// A step up on that side
				world.getServer().runCommand("fill 5 101 0 8 101 8 stone");
				exit = ride(context, world, vehicle, -90);
				check(exit.x > 5 && exit.y == 102, vehicle + ": up on the step (" + exit + ")");
				world.getServer().runCommand("fill 5 101 0 8 101 8 air");
			}
		}
	}

	private static Vec3 ride(ClientGameTestContext context, TestSingleplayerContext world, String vehicle, int yaw) {
		world.getServer().runCommand("kill @e[type=!player]");
		world.getServer().runCommand("summon " + vehicle + " 4.5 101 4.5 {NoGravity:1b}");
		world.getServer().runCommand("tp @a 4.5 101 4.5 " + yaw + " 0");
		context.waitTicks(2);
		world.getServer().runCommand("ride @p mount @e[type=" + vehicle + ",limit=1]");
		context.waitTicks(2);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			check(player.isPassenger(), vehicle + " ridden");
			player.setYRot(yaw);
			player.stopRiding();
			return player.position();
		});
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
