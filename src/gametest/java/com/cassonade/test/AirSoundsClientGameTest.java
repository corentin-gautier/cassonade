package com.cassonade.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import com.cassonade.client.ambience.AirSounds;
import com.cassonade.client.ambience.Birds;

/** What calls where by day and by night, and gulls rather than geese over the coast. */
public class AirSoundsClientGameTest implements FabricClientGameTest {
	/** Half of a flock's 400 blocks at 0.45 blocks a tick. */
	private static final int TICKS_TO_MIDDLE = 444;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			// Only what this test calls
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			check(context.computeOnClient(client -> client.getSoundManager().getSoundEvent(AirSounds.GULLS).getWeight() > 0), "the gulls have their sounds");
			check(context.computeOnClient(client -> client.getSoundManager().getSoundEvent(AirSounds.FOREST_DAY).getWeight() > 0), "the forest by day has its sounds");
			check(context.computeOnClient(client -> client.getSoundManager().getSoundEvent(AirSounds.FOREST_NIGHT).getWeight() > 0), "the forest by night has its sounds");
			check(context.computeOnClient(client -> client.getSoundManager().getSoundEvent(AirSounds.MEADOW).getWeight() > 0), "the meadow has its sounds");


			world.getServer().runCommand("time set 6000");
			// What calls where, at noon and at night
			// Robins and cuckoos only with a tree near
			waitForBirds(context, world, "forest", null);
			waitForBirds(context, world, "plains", null);
			world.getServer().runCommand("execute as @p at @s run fill ~3 ~3 ~-2 ~7 ~3 ~2 minecraft:oak_leaves[persistent=true]");
			context.waitTicks(5);
			waitForBirds(context, world, "forest", AirSounds.FOREST_DAY);
			waitForBirds(context, world, "plains", AirSounds.MEADOW);
			waitForBirds(context, world, "desert", null);
			waitForBirds(context, world, "jungle", null);
			// Gulls are no ambience: they call from a flock passing over the coast
			waitForBirds(context, world, "beach", null);
			// What a flock of gulls really plays: gulls, not the geese
			List<Identifier> heard = new CopyOnWriteArrayList<>();
			SoundEventListener listener = (sound, event, range) -> heard.add(sound.getIdentifier());
			context.runOnClient(client -> client.getSoundManager().addListener(listener));
			// Low enough to be heard
			context.runOnClient(client -> Birds.releaseGulls(client, 40));
			context.runOnClient(client -> Birds.advance(TICKS_TO_MIDDLE));
			context.waitFor(client -> heard.contains(AirSounds.GULLS), 20 * 5);
			context.runOnClient(client -> client.getSoundManager().removeListener(listener));
			check(!heard.contains(Birds.CALL), "gulls don't call like geese (" + heard + ")");
			// Sunset is night for them, while the sky is still bright
			world.getServer().runCommand("time set 13000");
			waitForBirds(context, world, "plains", null);
			world.getServer().runCommand("time set 18000");
			waitForBirds(context, world, "plains", null);
			waitForBirds(context, world, "forest", AirSounds.FOREST_NIGHT);
			waitForBirds(context, world, "snowy_plains", null);

			// Frogs in a swamp from dusk on, not by day
			world.getServer().runCommand("execute as @p at @s run fillbiome ~-8 -64 ~-8 ~8 -40 ~8 minecraft:swamp");
			context.waitFor(client -> AirSounds.frogsAt(client.level, client.player.blockPosition()), 20 * 5);
			Identifier frog = SoundEvents.FROG_AMBIENT.location();
			context.runOnClient(client -> client.getSoundManager().addListener(listener));
			context.waitFor(client -> heard.contains(frog), 20 * 15);
			context.runOnClient(client -> client.getSoundManager().removeListener(listener));
			world.getServer().runCommand("time set 6000");
			context.waitTicks(2);
			check(!context.computeOnClient(client -> AirSounds.frogsAt(client.level, client.player.blockPosition())), "no frogs by day");

			// Grasshoppers in a savanna by day, crickets at night in the plains, nothing by day there
			waitForInsects(context, world, "savanna", AirSounds.GRASSHOPPERS);
			hear(context, listener, heard, AirSounds.GRASSHOPPERS);
			waitForInsects(context, world, "plains", null);
			world.getServer().runCommand("time set 18000");
			waitForInsects(context, world, "plains", AirSounds.CRICKETS);
			hear(context, listener, heard, AirSounds.CRICKETS);
			waitForInsects(context, world, "savanna", AirSounds.CRICKETS);
			waitForInsects(context, world, "desert", null);
			waitForInsects(context, world, "snowy_plains", null);
			waitForInsects(context, world, "beach", null);
		}
	}

	private static void waitForInsects(ClientGameTestContext context, TestSingleplayerContext world, String biome, Identifier expected) {
		world.getServer().runCommand("execute as @p at @s run fillbiome ~-8 -64 ~-8 ~8 -40 ~8 minecraft:" + biome);
		context.waitFor(client -> client.level.getBiome(client.player.blockPosition()).is(Identifier.withDefaultNamespace(biome)), 20 * 5);
		context.waitTicks(3);
		check(java.util.Objects.equals(context.computeOnClient(client -> AirSounds.insectsAt(client.level, client.player.blockPosition())), expected), biome + " has " + expected);
	}

	/** What the game really plays once it is their turn. */
	private static void hear(ClientGameTestContext context, SoundEventListener listener, List<Identifier> heard, Identifier sound) {
		check(context.computeOnClient(client -> client.getSoundManager().getSoundEvent(sound).getWeight() > 0), sound + " has its sounds");
		context.runOnClient(client -> client.getSoundManager().addListener(listener));
		context.runOnClient(client -> AirSounds.callInsects());
		context.waitFor(client -> heard.contains(sound), 20);
		context.runOnClient(client -> client.getSoundManager().removeListener(listener));
	}

	private static void waitForBirds(ClientGameTestContext context, TestSingleplayerContext world, String biome, Identifier expected) {
		world.getServer().runCommand("execute as @p at @s run fillbiome ~-8 -64 ~-8 ~8 -40 ~8 minecraft:" + biome);
		context.waitFor(client -> client.level.getBiome(client.player.blockPosition()).is(Identifier.withDefaultNamespace(biome)), 20 * 5);
		context.waitTicks(3);
		check(java.util.Objects.equals(context.computeOnClient(client -> AirSounds.birdsAt(client.level, client.player.blockPosition())), expected), biome + " calls " + expected);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
