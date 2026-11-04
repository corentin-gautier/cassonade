package com.cassonade.test;

import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Mushroom fields: fairy rings of mushrooms on the mycelium, seen from above by day, and the spores rising
 * from the ground glowing at night.
 */
public class MushroomFieldsClientGameTest implements FabricClientGameTest {
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
			BlockPos fields = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome -> biome.is(Biomes.MUSHROOM_FIELDS), BlockPos.ZERO, 12800, 64, 64);
				if (found == null) {
					return null;
				}
				// The middle of the widest stretch of mycelium around, rather than its shore
				BlockPos best = null;
				int most = 0;
				for (int x = -96; x <= 96; x += 8) {
					for (int z = -96; z <= 96; z += 8) {
						int count = 0;
						for (int dx = -16; dx <= 16; dx += 4) {
							for (int dz = -16; dz <= 16; dz += 4) {
								int px = found.getFirst().getX() + x + dx;
								int pz = found.getFirst().getZ() + z + dz;
								// Generated on the way: the area is mostly far from any player
								int height = level.getChunk(px >> 4, pz >> 4).getHeight(Heightmap.Types.WORLD_SURFACE, px & 15, pz & 15);
								BlockPos top = new BlockPos(px, height, pz);
								if (level.getBlockState(top).is(Blocks.MYCELIUM)) {
									count++;
								}
							}
						}
						if (count > most) {
							most = count;
							best = found.getFirst().offset(x, 0, z);
						}
					}
				}
				return best;
			});
			if (fields == null) {
				throw new AssertionError("Expected: mushroom fields in this world");
			}
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("tp @a " + fields.getX() + " 140 " + fields.getZ() + " 0 90");
			context.waitTicks(60);
			world.getConnection().waitForChunksRender();
			int top = world.getServer().computeOnServer(server -> server.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, fields.getX(), fields.getZ()));
			world.getServer().runCommand("tp @a " + fields.getX() + " " + (top + 30) + " " + fields.getZ() + " 0 90");
			context.waitTicks(40);
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> client.gui.hud.toggle());
			Screenshots.take(context, "mushroom-fields-above");
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("tp @a " + fields.getX() + " " + (top + 2) + " " + fields.getZ() + " 0 10");
			context.waitTicks(100);
			Screenshots.take(context, "mushroom-fields-night");
		}
	}
}
