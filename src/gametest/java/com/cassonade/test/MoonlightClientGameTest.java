package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;

import com.cassonade.time.FullMoon;

/** Nights lighter under a full moon and darker under a new one, and full moon nights free of monsters on the surface. */
public class MoonlightClientGameTest implements FabricClientGameTest {
	/** Midnight, on the first night of the moon's cycle (full), the third (quarter) and the fifth (new). */
	private static final int FULL = 18000;
	private static final int QUARTER = 2 * 24000 + 18000;
	private static final int NEW = 4 * 24000 + 18000;
	private static final BlockPos SURFACE = new BlockPos(0, 100, 6);
	private static final BlockPos CAVE = new BlockPos(0, 90, 6);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("fill -12 89 -4 12 99 30 stone");
			world.getServer().runCommand("fill -12 100 -4 12 110 30 air");
			world.getServer().runCommand("fill -2 90 5 2 92 7 air");
			world.getServer().runCommand("kill @e[type=!player]");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("tp @a 0.5 101.6 0.5 0 10");
			context.runOnClient(client -> client.gui.hud.toggle());

			float full = night(context, world, FULL, "moonlight-full");
			float quarter = night(context, world, QUARTER, "moonlight-quarter");
			float darkest = night(context, world, NEW, "moonlight-new");
			check(full > quarter && quarter > darkest, "nights lighter at full moon and darker at new moon: " + full + ", " + quarter + ", " + darkest);

			world.getServer().runCommand("time set " + FULL);
			check(world.getServer().computeOnServer(server -> FullMoon.peaceful(server.overworld(), EntityTypes.ZOMBIE, SURFACE)),
					"no zombie on the surface under a full moon");
			check(!world.getServer().computeOnServer(server -> FullMoon.peaceful(server.overworld(), EntityTypes.ZOMBIE, CAVE)),
					"zombies in caves under a full moon");
			check(!world.getServer().computeOnServer(server -> FullMoon.peaceful(server.overworld(), EntityTypes.SLIME, SURFACE)),
					"slimes on the surface under a full moon");
			world.getServer().runCommand("time set " + NEW);
			check(!world.getServer().computeOnServer(server -> FullMoon.peaceful(server.overworld(), EntityTypes.ZOMBIE, SURFACE)),
					"zombies on the surface under a new moon");
		}
	}

	private static float night(ClientGameTestContext context, TestSingleplayerContext world, int time, String shot) {
		world.getServer().runCommand("time set " + time);
		context.waitTicks(10);
		world.getConnection().waitForChunksRender();
		Screenshots.take(context, shot);
		return context.computeOnClient(client -> client.level.environmentAttributes().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, Vec3.atCenterOf(SURFACE)));
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
