package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.levelgen.Heightmap;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.MorningMist;

/** In a generated world, at the shore of the nearest ocean and near a real ocean monument: nothing of the sky or of what lies under the sea shows through far water, with or without the mist. */
public class SeaHorizonClientGameTest implements FabricClientGameTest {
	private static final float EAST = -90;
	private static final float WEST = 90;
	private static final float NORTH = 180;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setSeed("vanillasugar");
			state.setWorldType(state.getNormalPresetList().getFirst());
		}).create()) {
			context.waitTicks(100);
			context.runOnClient(client -> {
				Birds.setSpawning(false);
				client.options.renderDistance().set(16);
				// Or the sea stops short at the test's low view distance
				client.options.broadcastOptions();
			});
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamemode spectator @a");

			BlockPos shore = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				return level.findClosestBiome3d(biome -> biome.is(BiomeTags.IS_OCEAN), level.getRespawnData().pos(), 6400, 32, 64).getFirst();
			});
			BlockPos monument = world.getServer().computeOnServer(server ->
					server.overworld().findNearestMapStructure(StructureTags.ON_OCEAN_MONUMENT_MAPS, shore, 100, false));

			// With the mist first, as what it shows must not change
			long sunrise = MistyMorning.sunrise();
			shot(context, world, sunrise, shore, 75, EAST, 0, "cassonade-sea-shore-sunrise");
			context.waitFor(client -> MorningMist.density() == 1, 20 * 30);
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-sea-shore-mist");

			long day = sunrise + 6000;
			shot(context, world, day, shore, 75, WEST, 5, "cassonade-sea-shore-day");
			shot(context, world, sunrise + 12700, shore, 75, WEST, 3, "cassonade-sea-shore-sunset");
			// Just above the water, where the look skims it, and high enough to see the last chunks' arc
			shot(context, world, sunrise + 12700, shore, 66, WEST, 3, "cassonade-sea-low-sunset");
			shot(context, world, sunrise + 12700, shore, 100, WEST, 8, "cassonade-sea-arc-sunset");
			shot(context, world, sunrise + 18000, shore, 75, WEST, 3, "cassonade-sea-shore-night");

			// From high enough to see the sea's far edge, a monument near that edge
			BlockPos off = monument.south(170);
			shot(context, world, day, off, 90, NORTH, 8, "cassonade-sea-monument-day");
			// Just above open water
			shot(context, world, sunrise + 12700, off, 66, NORTH, 2, "cassonade-sea-open-low");
			// Under water, facing the sunset: only the water's color in the distance
			shot(context, world, sunrise + 12700, off, 58, WEST, 0, "cassonade-sea-underwater-sunset");
			shot(context, world, day, off, 58, WEST, 0, "cassonade-sea-underwater-day");
			int seabed = world.getServer().computeOnServer(server -> server.overworld().getHeight(Heightmap.Types.OCEAN_FLOOR, off.getX(), off.getZ()));
			shot(context, world, day, off, seabed + 2, WEST, 0, "cassonade-sea-underwater-deep-day");
			// A flooded cave under the sea, its water's surface under rock: no sky in it
			int cave = seabed + 2;
			String x1 = (off.getX() - 6) + " ", x2 = (off.getX() + 6) + " ", z1 = " " + (off.getZ() - 6), z2 = " " + (off.getZ() + 6);
			world.getServer().runCommand("fill " + x1 + cave + z1 + " " + x2 + (cave + 8) + z2 + " stone hollow");
			world.getServer().runCommand("fill " + x1 + (cave + 1) + z1 + " " + x2 + (cave + 5) + z2 + " water replace air");
			shot(context, world, day, off, cave + 2, NORTH, -75, "cassonade-sea-flooded-cave");
			world.getServer().runCommand("fill " + x1 + cave + z1 + " " + x2 + (cave + 8) + z2 + " water");
			// Looking up, the sky shows only in a window overhead
			shot(context, world, day, off, 58, NORTH, -75, "cassonade-sea-underwater-up-day");
			shot(context, world, sunrise + 12700, off, 58, WEST, -50, "cassonade-sea-underwater-up-sunset");
			shot(context, world, sunrise + 12700, off, 180, WEST, 12, "cassonade-sea-high-sunset");
		}
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext world, long time, BlockPos at, int y, float yaw, float pitch, String name) {
		world.getServer().runCommand("time set " + time);
		world.getServer().runCommand("tp @a " + at.getX() + " " + y + " " + at.getZ() + " " + yaw + " " + pitch);
		world.getConnection().waitForClientboundPackets();
		// Not waitForChunksRender: at this view distance over open sea it can outlast its timeout
		context.waitTicks(300);
		Screenshots.take(context, name);
	}
}
