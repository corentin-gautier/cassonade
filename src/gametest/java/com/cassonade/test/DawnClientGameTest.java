package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.dev.DevRendering;

/** Dawn seen from high over a generated world: the land's far edge stays an arc and the sun's glow fades without a border. */
public class DawnClientGameTest implements FabricClientGameTest {
	private static final float EAST = -90;
	private static final float WEST = 90;

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
				client.options.renderDistance().set(12);
				client.options.broadcastOptions();
			});
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamemode spectator @a");
			BlockPos meadow = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				return level.findClosestBiome3d(biome -> biome.is(Biomes.MEADOW), level.getRespawnData().pos(), 6400, 32, 64).getFirst();
			});
			world.getServer().runCommand("time set 22400");
			world.getServer().runCommand("tp @a " + meadow.getX() + " 148 " + meadow.getZ() + " " + EAST + " 12");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			Screenshots.take(context, "cassonade-dawn-loading");
			context.waitTicks(300);
			Screenshots.take(context, "cassonade-dawn-east");
			// The same view as the game draws it, to compare
			CompletableFuture<Void> vanilla = context.computeOnClient(DevRendering::toggle);
			context.waitFor(client -> vanilla.isDone(), 20 * 60);
			context.waitTicks(300);
			Screenshots.take(context, "cassonade-dawn-east-vanilla");
			CompletableFuture<Void> back = context.computeOnClient(DevRendering::toggle);
			context.waitFor(client -> back.isDone(), 20 * 60);
			context.waitTicks(300);
			world.getServer().runCommand("tp @a " + meadow.getX() + " 148 " + meadow.getZ() + " " + (EAST + 60) + " 12");
			context.waitTicks(300);
			Screenshots.take(context, "cassonade-dawn-southeast");
			// By day, far land on both sides of the sea, the mod's then the game's
			world.getServer().runCommand("time set 1000");
			world.getServer().runCommand("tp @a " + meadow.getX() + " 148 " + meadow.getZ() + " " + EAST + " 20");
			context.waitTicks(60);
			Screenshots.take(context, "cassonade-day-east");
			// Low over the sea by day, where the horizon's haze shows
			world.getServer().runCommand("tp @a " + meadow.getX() + " 148 " + meadow.getZ() + " " + EAST + " -10");
			context.waitTicks(20);
			Screenshots.take(context, "cassonade-day-sky");
			world.getServer().runCommand("tp @a " + meadow.getX() + " 148 " + meadow.getZ() + " " + EAST + " 20");
			context.waitTicks(20);
			CompletableFuture<Void> vanillaDay = context.computeOnClient(DevRendering::toggle);
			context.waitFor(client -> vanillaDay.isDone(), 20 * 60);
			context.waitTicks(300);
			Screenshots.take(context, "cassonade-day-east-vanilla");
			CompletableFuture<Void> backDay = context.computeOnClient(DevRendering::toggle);
			context.waitFor(client -> backDay.isDone(), 20 * 60);
			context.waitTicks(300);
			world.getServer().runCommand("time set 23000");
			world.getServer().runCommand("tp @a " + meadow.getX() + " 148 " + meadow.getZ() + " " + EAST + " 6");
			context.waitTicks(60);
			Screenshots.take(context, "cassonade-dawn-sun");
			// The full moon setting on the other side
			world.getServer().runCommand("tp @a " + meadow.getX() + " 148 " + meadow.getZ() + " " + WEST + " 2");
			context.waitTicks(40);
			Screenshots.take(context, "cassonade-dawn-moon");
		}
	}
}
