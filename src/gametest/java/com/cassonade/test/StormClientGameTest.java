package com.cassonade.test;

import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Storm;
import com.cassonade.weather.SandDrifts;

/**
 * A sandstorm in the desert and the badlands, a blizzard in the snowy plains, when it rains, thicker in a thunderstorm; none in
 * the plains, and none indoors. The sandstorm lays sand on the dunes, up to three layers in a pit, none on a build.
 */
public class StormClientGameTest implements FabricClientGameTest {
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
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("gamemode spectator @a");
			context.runOnClient(client -> client.gui.hud.toggle());

			goTo(context, world, Biomes.PLAINS);
			world.getServer().runCommand("weather rain");
			context.waitTicks(200);
			check(context.computeOnClient(client -> Storm.density() == 0 && Storm.windVolume() == 0), "no storm in the plains' rain, nor its wind");

			goTo(context, world, Biomes.DESERT);
			world.getServer().runCommand("weather clear");
			context.waitFor(client -> Storm.density() == 0, 20 * 15);
			Screenshots.take(context, "storm-desert-clear");
			world.getServer().runCommand("weather rain");
			context.waitFor(client -> Math.abs(Storm.density() - 0.7F) < 0.01F, 20 * 20);
			Screenshots.take(context, "storm-desert-rain");
			// The wind blows with the storm, all around
			context.waitFor(client -> Storm.windVolume() > 0.1F, 20 * 10);
			world.getServer().runCommand("weather thunder");
			context.waitFor(client -> Storm.density() > 0.99F, 20 * 20);
			Screenshots.take(context, "storm-desert-thunder");
			// The thunder turns the sand's rumble to a gale
			context.waitFor(client -> Storm.windVolume(Storm.WIND_THUNDER) > 0.3F && Storm.windVolume(Storm.WIND) == 0, 20 * 30);
			world.getServer().runCommand("time set 18000");
			context.waitTicks(20);
			Screenshots.take(context, "storm-desert-night");
			world.getServer().runCommand("time set 6000");

			// The wind lays sand on the dunes, up to three layers, and none on what was built
			world.getServer().runCommand("execute as @p at @s run fill ~2 ~-2 ~2 ~4 ~-2 ~4 minecraft:sandstone");
			world.getServer().runCommand("execute as @p at @s run fill ~2 ~-1 ~2 ~4 ~10 ~4 minecraft:air");
			// A pit in the sand, where the wind can't sweep it out
			world.getServer().runCommand("execute as @p at @s run fill ~-5 ~-3 ~-5 ~-3 ~-3 ~-3 minecraft:sand");
			world.getServer().runCommand("execute as @p at @s run fill ~-5 ~-2 ~-5 ~-3 ~-2 ~-3 minecraft:sand");
			world.getServer().runCommand("execute as @p at @s run setblock ~-4 ~-2 ~-4 minecraft:air");
			// Nothing above it: on a dune, the sand over the pit would fall in and fill it
			world.getServer().runCommand("execute as @p at @s run fill ~-5 ~-1 ~-5 ~-3 ~10 ~-3 minecraft:air");
			world.getServer().runCommand("gamerule random_tick_speed 3000");
			context.waitTicks(200);
			world.getServer().runCommand("gamerule random_tick_speed 3");
			int[] layers = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				BlockPos player = server.getPlayerList().getPlayers().getFirst().blockPosition();
				int[] found = new int[5];
				found[4] = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, player.offset(-4, 0, -4)).getY() - player.getY();
				BlockState pit = level.getBlockState(player.offset(-4, -2, -4));
				found[3] = pit.is(SandDrifts.SAND_LAYER) ? pit.getValue(SnowLayerBlock.LAYERS) : 0;
				for (BlockPos pos : BlockPos.betweenClosed(player.offset(-24, -12, -24), player.offset(24, 12, 24))) {
					BlockState state = level.getBlockState(pos);
					if (state.is(SandDrifts.SAND_LAYER)) {
						found[0]++;
						found[1] = Math.max(found[1], state.getValue(SnowLayerBlock.LAYERS));
						found[2] += level.getBlockState(pos.below()).is(Blocks.SANDSTONE) ? 1 : 0;
					}
				}
				return found;
			});
			check(layers[0] > 50, "sand drifts over the dunes, found " + layers[0]);
			check(layers[1] == SandDrifts.STORM_LAYERS, "the storm piles sand up to three layers, found " + layers[1]);
			check(layers[2] == 0, "no sand on sandstone");
			check(layers[3] == SandDrifts.STORM_LAYERS, "a pit fills up to three layers, found " + layers[3] + ", its column's top " + layers[4] + " from the feet");
			int dropped = world.getServer().computeOnServer(server -> Block.getDrops(SandDrifts.SAND_LAYER.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 3),
					server.overworld(), BlockPos.ZERO, null).stream().mapToInt(stack -> stack.is(SandDrifts.SAND_LAYER_ITEM) ? stack.getCount() : 0).sum());
			check(dropped == 3, "three layers of sand broken give three, got " + dropped);
			Screenshots.take(context, "storm-desert-drifts");

			// Shut in, out of the sky's light: the storm stays outside, and its wind
			world.getServer().runCommand("execute as @p at @s run fill ~-8 ~-4 ~-8 ~8 ~8 ~8 minecraft:sandstone hollow");
			context.waitFor(client -> Storm.density() == 0 && Storm.windVolume() == 0, 20 * 15);
			world.getServer().runCommand("execute as @p at @s run fill ~-8 ~-4 ~-8 ~8 ~8 ~8 minecraft:air");

			goTo(context, world, Biomes.BADLANDS);
			context.waitFor(client -> Storm.density() > 0.99F, 20 * 20);
			context.waitTicks(100);
			Screenshots.take(context, "storm-badlands-thunder");

			goTo(context, world, Biomes.SNOWY_PLAINS);
			world.getServer().runCommand("weather rain");
			context.waitFor(client -> Math.abs(Storm.density() - 0.7F) < 0.01F, 20 * 20);
			context.waitTicks(100);
			Screenshots.take(context, "storm-snow-rain");
			// A blizzard's own wind, calm in the rain, where the sand's has died away
			context.waitFor(client -> Storm.windVolume(Storm.BLIZZARD) > 0.1F && Storm.windVolume(Storm.WIND) == 0
					&& Storm.windVolume(Storm.BLIZZARD_THUNDER) == 0, 20 * 30);
			world.getServer().runCommand("weather thunder");
			context.waitFor(client -> Storm.density() > 0.99F, 20 * 20);
			Screenshots.take(context, "storm-snow-thunder");
			// The thunder brings the gale in its place
			context.waitFor(client -> Storm.windVolume(Storm.BLIZZARD_THUNDER) > 0.2F && Storm.windVolume(Storm.BLIZZARD) == 0, 20 * 30);
		}
	}

	/** Standing on the ground somewhere in that biome, looking a little down across it. */
	private static void goTo(ClientGameTestContext context, TestSingleplayerContext world, ResourceKey<Biome> biome) {
		BlockPos spot = world.getServer().computeOnServer(server -> {
			ServerLevel level = server.overworld();
			Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(candidate -> candidate.is(biome), BlockPos.ZERO, 12800, 64, 64);
			if (found == null) {
				return null;
			}
			// Dry land some way inside, rather than a river or the biome's edge
			for (int ring = 0; ring <= 256; ring += 8) {
				for (int x = -ring; x <= ring; x += 8) {
					for (int z = -ring; z <= ring; z += 8) {
						if (Math.max(Math.abs(x), Math.abs(z)) != ring) {
							continue;
						}
						int px = found.getFirst().getX() + x;
						int pz = found.getFirst().getZ() + z;
						int height = level.getChunk(px >> 4, pz >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING, px & 15, pz & 15);
						BlockPos top = new BlockPos(px, height, pz);
						if (open(level, top) && level.getBiome(top).is(biome)
								&& level.getBiome(top.offset(24, 0, 0)).is(biome) && level.getBiome(top.offset(-24, 0, 0)).is(biome)
								&& level.getBiome(top.offset(0, 0, 24)).is(biome) && level.getBiome(top.offset(0, 0, -24)).is(biome)) {
							return top;
						}
					}
				}
			}
			return null;
		});
		if (spot == null) {
			throw new AssertionError("Expected: dry land in " + biome.identifier() + " in this world");
		}
		world.getServer().runCommand("tp @a " + spot.getX() + " " + (spot.getY() + 1) + " " + spot.getZ() + " 0 10");
		world.getConnection().waitForClientboundPackets();
		context.waitTicks(40);
		world.getConnection().waitForChunksRender();
	}

	/** Fairly flat dry ground all around, within three blocks of this height over 16 blocks each way, for a view across it. */
	private static boolean open(ServerLevel level, BlockPos top) {
		for (int x = -16; x <= 16; x += 4) {
			for (int z = -16; z <= 16; z += 4) {
				int px = top.getX() + x;
				int pz = top.getZ() + z;
				int height = level.getChunk(px >> 4, pz >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING, px & 15, pz & 15);
				if (Math.abs(height - top.getY()) > 3 || !level.getFluidState(new BlockPos(px, height - 1, pz)).isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
