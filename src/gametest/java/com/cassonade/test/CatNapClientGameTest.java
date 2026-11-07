package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.phys.AABB;

/** By day a stray cat walks to a hay bale close by and sleeps on it, or lies down where it is with none around; at night it stays up. */
public class CatNapClientGameTest implements FabricClientGameTest {
	private static final AABB AREA = new AABB(-20, 90, -20, 20, 120, 40);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("fill -8 99 -8 8 99 30 stone");
			world.getServer().runCommand("fill -8 100 -8 8 110 30 air");
			world.getServer().runCommand("kill @e[type=!player]");
			world.getServer().runCommand("tp @a 0.5 104 -6.5 0 30");

			// At night, a cat with a hay bale next to it stays up
			world.getServer().runCommand("time set midnight");
			world.getServer().runCommand("setblock 0 100 12 hay_block");
			world.getServer().runCommand("summon cat 0.5 100 6.5");
			check(!lies(context, world, 600, false), "a cat staying up at night");

			// By day, it walks to the hay bale and sleeps on it
			world.getServer().runCommand("time set noon");
			check(lies(context, world, 600, true), "a cat sleeping on the hay bale by day");
			context.takeScreenshot("cat-nap");

			// With no soft spot around, it lies down where it is
			world.getServer().runCommand("kill @e[type=cat]");
			world.getServer().runCommand("setblock 0 100 12 air");
			world.getServer().runCommand("summon cat 0.5 100 6.5");
			check(lies(context, world, 1200, false), "a cat dozing on the ground by day");
		}
	}

	/** Waits up to the given ticks for the cat to lie down, on the hay bale if asked. */
	private static boolean lies(ClientGameTestContext context, TestSingleplayerContext world, int ticks, boolean onHay) {
		for (int tick = 0; tick < ticks; tick++) {
			context.waitTick();
			boolean lying = world.getServer().computeOnServer(server -> {
				Cat cat = server.overworld().getEntitiesOfClass(Cat.class, AREA).getFirst();
				return cat.isLying() && (!onHay || cat.blockPosition().equals(new BlockPos(0, 101, 12)));
			});
			if (lying) {
				return true;
			}
		}
		return false;
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
