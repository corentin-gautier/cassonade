package com.cassonade.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.AirSounds;
import com.cassonade.client.ambience.Aurora;
import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Butterflies;
import com.cassonade.client.ambience.MorningMist;
import com.cassonade.client.ambience.Motes;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

/**
 * The birds' sounds exist; the Milky Way by night; the aurora on an odd night over snow; the mist on its morning, gone by
 * noon; butterflies around flowers; dust in a cave, not in a hut.
 */
public class AmbienceClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			// Only the flocks this test releases
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			check(context.computeOnClient(client -> client.getSoundManager().getSoundEvent(AirSounds.GULLS).getWeight() > 0), "the gulls have their sounds");
			check(context.computeOnClient(client -> client.getSoundManager().getSoundEvent(AirSounds.FOREST_DAY).getWeight() > 0), "the forest by day has its sounds");
			check(context.computeOnClient(client -> client.getSoundManager().getSoundEvent(AirSounds.FOREST_NIGHT).getWeight() > 0), "the forest by night has its sounds");
			check(context.computeOnClient(client -> client.getSoundManager().getSoundEvent(AirSounds.MEADOW).getWeight() > 0), "the meadow has its sounds");

			// A clear night, looking at the Milky Way's bulge, high to the north
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~ ~ 180 -55");
			context.waitTicks(10);
			Screenshots.take(context, "cassonade-ambience-milky-way");

			// The next night over snow, looking north
			world.getServer().runCommand("execute as @p at @s run fillbiome ~-8 -64 ~-8 ~8 -40 ~8 minecraft:snowy_plains");
			world.getServer().runCommand("time set 42000");
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~ ~ 180 -25");
			world.getConnection().waitForClientboundPackets();
			context.waitFor(client -> Aurora.strength() >= 1, 20 * 10);
			Screenshots.take(context, "cassonade-ambience-aurora");

			// A misty morning's dawn
			world.getServer().runCommand("time set " + (MistyMorning.sunrise() - 200));
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~ ~ 0 0");
			context.waitFor(client -> MorningMist.density() > 0.9F, 20 * 15);
			Screenshots.take(context, "cassonade-ambience-mist");
			// A thick canopy overhead takes the sky's light at the player's block, and leaves the mist as it was
			world.getServer().runCommand("execute as @p at @s run fill ~-2 ~3 ~-2 ~2 ~9 ~2 minecraft:oak_leaves[persistent=true]");
			context.waitTicks(40);
			check(context.computeOnClient(client -> MorningMist.density()) > 0.9F, "the mist doesn't change under a tree");
			world.getServer().runCommand("execute as @p at @s run fill ~-2 ~3 ~-2 ~2 ~9 ~2 minecraft:air");
			world.getServer().runCommand("time set 6000");
			context.waitFor(client -> MorningMist.density() == 0, 20 * 15);

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
			// What is really played there: gulls, never the geese
			List<Identifier> heard = new CopyOnWriteArrayList<>();
			SoundEventListener listener = (sound, event, range) -> heard.add(sound.getIdentifier());
			context.runOnClient(client -> client.getSoundManager().addListener(listener));
			// Low enough to be heard
			context.runOnClient(client -> Birds.release(client, 40));
			context.waitFor(client -> heard.contains(AirSounds.GULLS), 20 * 60);
			context.waitTicks(20 * 15);
			context.runOnClient(client -> client.getSoundManager().removeListener(listener));
			check(!heard.contains(Birds.CALL), "no geese on the beach (" + heard + ")");
			// Sunset is night for them, while the sky is still bright
			world.getServer().runCommand("time set 13000");
			waitForBirds(context, world, "plains", null);
			world.getServer().runCommand("time set 18000");
			waitForBirds(context, world, "plains", null);
			waitForBirds(context, world, "forest", AirSounds.FOREST_NIGHT);
			waitForBirds(context, world, "snowy_plains", null);
			world.getServer().runCommand("execute as @p at @s run fill ~3 ~3 ~-2 ~7 ~3 ~2 minecraft:air");
			world.getServer().runCommand("time set 6000");

			// Flowers around, in a temperate biome
			world.getServer().runCommand("execute as @p at @s run fillbiome ~-8 -64 ~-8 ~8 -40 ~8 minecraft:plains");
			world.getServer().runCommand("execute as @p at @s run fill ~-6 ~ ~-6 ~6 ~ ~6 minecraft:poppy replace air");
			context.waitFor(client -> Butterflies.count() > 0, 20 * 30);
			// Once fully grown in, from up close
			context.waitTicks(40);
			world.getServer().runCommand("gamemode spectator @a");
			Vec3 first = context.computeOnClient(client -> Butterflies.first());
			world.getServer().runCommand("tp @a " + first.x + " " + (first.y - 0.6) + " " + (first.z - 1.4) + " 0 20");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(1);
			Screenshots.take(context, "cassonade-ambience-butterfly");

			// Bees, adult and baby, drawn smaller; beside an oak log for scale
			world.getServer().runCommand("setblock 20 -59 24 minecraft:oak_log");
			world.getServer().runCommand("summon minecraft:bee 19 -59 24 {NoAI:1b}");
			world.getServer().runCommand("summon minecraft:bee 21.5 -59 24 {NoAI:1b,Age:-24000}");
			world.getServer().runCommand("tp @a 20.5 -58 21.5 0 10");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			Screenshots.take(context, "cassonade-ambience-bees");

			// Savanna, too hot for the temperate pollen, has its own air
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("execute as @p at @s run fillbiome ~-16 ~-3 ~-16 ~16 ~8 ~16 minecraft:savanna");
			context.waitFor(client -> client.level.getBiome(client.player.blockPosition()).is(Identifier.withDefaultNamespace("savanna")), 20 * 5);
			context.waitTicks(100);
			Screenshots.take(context, "cassonade-ambience-savanna");

			// Dust falling from the ceiling of a lit cave, with rock above it; none in a hut on the surface
			world.getServer().runCommand("fill 40 -60 40 50 -44 50 minecraft:stone");
			world.getServer().runCommand("fill 41 -59 41 49 -55 49 minecraft:air");
			world.getServer().runCommand("fill 60 -60 60 66 -56 66 minecraft:stone hollow");
			world.getServer().runCommand("setblock 45 -55 45 minecraft:sea_lantern");
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("tp @a 45.5 -59 41.5 0 -5");
			world.getConnection().waitForClientboundPackets();
			world.getConnection().waitForChunksRender();
			context.waitTicks(200);
			check(context.computeOnClient(client -> Motes.inCave(client.level, new BlockPos(45, -58, 45))), "under rock is a cave");
			check(!context.computeOnClient(client -> Motes.inCave(client.level, new BlockPos(63, -58, 63))), "a hut is no cave");
			check(context.computeOnClient(client -> Motes.underCeiling(client.level, new BlockPos(44, -55, 45))), "dust starts under the rock");
			check(!context.computeOnClient(client -> Motes.underCeiling(client.level, new BlockPos(44, -58, 45))), "not in mid-air");
			Screenshots.take(context, "cassonade-ambience-dust");
			context.runOnClient(client -> Birds.setSpawning(true));
		}
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
