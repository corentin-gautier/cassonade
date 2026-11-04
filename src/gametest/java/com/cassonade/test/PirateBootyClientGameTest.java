package com.cassonade.test;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import com.cassonade.treasure.PirateBooty;

/**
 * Pirate's booty: piles of barrels with their loot table on lush caves' floors, found around a fixed seed's lush
 * caves, few of them for the caves' size, and a screenshot of one.
 */
public class PirateBootyClientGameTest implements FabricClientGameTest {
	/** Chunks searched in every direction around the lush caves found. */
	private static final int RADIUS = 12;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setSeed("vanillasugar");
			state.setWorldType(state.getNormalPresetList().getFirst());
			state.setAllowCommands(true);
		}).create()) {
			world.getConnection().waitForChunksRender();
			List<BlockPos> hoards = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome -> biome.is(Biomes.LUSH_CAVES), BlockPos.ZERO, 6400, 32, 64);
				if (found == null) {
					throw new AssertionError("Expected lush caves in this world");
				}
				int cx = found.getFirst().getX() >> 4;
				int cz = found.getFirst().getZ() >> 4;
				List<BlockPos> barrels = new ArrayList<>();
				int lush = 0;
				for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
					for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
						LevelChunk chunk = level.getChunk(x, z);
						for (int y = -48; y <= 48; y += 16) {
							if (level.getBiome(new BlockPos((x << 4) + 8, y, (z << 4) + 8)).is(Biomes.LUSH_CAVES)) {
								lush++;
								break;
							}
						}
						for (BlockEntity entity : chunk.getBlockEntities().values()) {
							if (entity instanceof BarrelBlockEntity barrel && PirateBooty.LOOT.equals(barrel.getLootTable())) {
								barrels.add(barrel.getBlockPos());
							}
						}
					}
				}
				// Barrels within a few blocks of each other make one hoard
				List<BlockPos> piles = new ArrayList<>();
				for (BlockPos barrel : barrels) {
					if (piles.stream().noneMatch(pile -> pile.distManhattan(barrel) <= 6)) {
						piles.add(barrel);
					}
				}
				System.out.println("Pirate's booty: " + piles.size() + " hoards, " + barrels.size() + " barrels, in " + lush + " chunks of lush caves out of "
						+ (2 * RADIUS + 1) * (2 * RADIUS + 1) + ": " + piles);
				return piles;
			});
			if (hoards.isEmpty()) {
				throw new AssertionError("Expected at least one pirate's booty around the lush caves");
			}
			BlockPos hoard = hoards.getFirst();
			// Somewhere open in the cave a few blocks from the pile, to look at it from
			BlockPos eye = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				for (int distance = 5; distance >= 2; distance--) {
					for (int dy = 2; dy >= 0; dy--) {
						for (Direction side : Direction.Plane.HORIZONTAL) {
							BlockPos spot = hoard.relative(side, distance).relative(side.getClockWise(), distance / 2).above(dy);
							if (level.getBlockState(spot).isAir() && level.getBlockState(spot.above()).isAir()) {
								return spot;
							}
						}
					}
				}
				return hoard.above(2);
			});
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("effect give @a minecraft:night_vision infinite 0 true");
			world.getServer().runCommand("tp @a " + (eye.getX() + 0.5) + " " + eye.getY() + " " + (eye.getZ() + 0.5) + " facing " + (hoard.getX() + 0.5) + " "
					+ hoard.getY() + " " + (hoard.getZ() + 0.5));
			world.getServer().runCommand("time set 6000");
			context.waitTicks(40);
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			Screenshots.take(context, "pirate-booty");
		}
	}
}
