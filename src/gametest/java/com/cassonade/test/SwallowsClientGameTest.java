package com.cassonade.test;

import java.util.HashSet;
import java.util.Set;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Robins;
import com.cassonade.client.ambience.Sparrows;
import com.cassonade.client.ambience.Swallows;
import com.cassonade.swallow.SwallowNests;
import com.cassonade.tweaks.HouseFoundations;

/**
 * Swallows: none without a nest; with a nest placed under a house's eave, a pair comes by day and sweeps over the roof, never
 * through a block, and one goes into the nest; the nest falls with its wall. Villages of a fixed seed hold a few nests.
 */
public class SwallowsClientGameTest implements FabricClientGameTest {
	private static final int SIDE = 14;
	private static final int VILLAGES = 4;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				Birds.setSpawning(false);
				Robins.setSpawning(false);
				Sparrows.setSpawning(false);
				Swallows.setSpawning(false);
			});
			world.getServer().runCommand("gamerule send_command_feedback false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 6000");
			BlockPos feet = context.computeOnClient(client -> client.player.blockPosition());
			// Cleared in slices, under the fill command's limit
			for (int z = -36; z <= 44; z += 8) {
				world.getServer().runCommand(fill(feet.offset(-36, 0, z), feet.offset(36, 24, z + 7), "air"));
				world.getServer().runCommand(fill(feet.offset(-36, -1, z), feet.offset(36, -1, z + 7), "grass_block"));
			}

			// A house whose roof juts out a block over its walls, three blocks high
			BlockPos house = feet.offset(-SIDE / 2, 0, 10);
			world.getServer().runCommand(fill(house, house.offset(SIDE, 2, SIDE), "oak_planks"));
			world.getServer().runCommand(fill(house.offset(1, 0, 1), house.offset(SIDE - 1, 2, SIDE - 1), "air"));
			world.getServer().runCommand(fill(house.offset(-1, 3, -1), house.offset(SIDE + 1, 3, SIDE + 1), "spruce_planks"));
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(5);

			// Without a nest, no swallow
			context.runOnClient(client -> {
				Swallows.rescan();
				Swallows.setSpawning(true);
			});
			context.waitTicks(20 * 4);
			check(context.computeOnClient(client -> Swallows.nests() == 0 && Swallows.count() == 0), "no swallow without a nest");
			context.runOnClient(client -> Swallows.setSpawning(false));

			// A nest under the eave of the house's north wall, facing out
			BlockPos nest = house.offset(SIDE / 2, 2, -1);
			world.getServer().runCommand("setblock " + nest.getX() + " " + nest.getY() + " " + nest.getZ() + " cassonade:swallow_nest[facing=north]");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(3);
			context.runOnClient(client -> Swallows.rescan());
			context.waitTicks(2);
			check(context.computeOnClient(client -> Swallows.nests() == 1 && nest.equals(Swallows.nearestNest(Vec3.atCenterOf(nest)))), "the nest found");

			// Up close to a nest, then the pair over the roof
			world.getServer().runCommand("gamemode spectator @a");
			Vec3 out = Vec3.atCenterOf(nest).add(0, 0, -2.5);
			look(context, world, out.add(0, -0.3, 0), Vec3.atCenterOf(nest).add(0, 0.3, 0));
			Screenshots.take(context, "cassonade-swallows-nest");

			context.runOnClient(client -> Swallows.setSpawning(true));
			look(context, world, Vec3.atCenterOf(house.offset(SIDE / 2, 0, -12)).add(0, 4, 0), Vec3.atCenterOf(nest).add(0, 4, 0));
			context.waitFor(client -> Swallows.count() == 2, 20 * 10);
			for (int tick = 0; tick < 100; tick++) {
				check(context.computeOnClient(client -> !Swallows.anyInBlock()), "the swallows out of the blocks, tick " + tick);
				context.waitTicks(1);
			}
			Screenshots.take(context, "cassonade-swallows-flying");
			context.runOnClient(client -> Swallows.visitNow());
			context.waitFor(client -> Swallows.nesting() == 1, 20 * 6);
			// From the side and a little below, where its folded wings show
			Vec3 side = Vec3.atCenterOf(nest).add(out.subtract(Vec3.atCenterOf(nest)).scale(0.6)).add(out.subtract(Vec3.atCenterOf(nest)).yRot((float) Math.PI / 2).scale(0.5));
			look(context, world, side.add(0, -0.4, 0), Vec3.atCenterOf(nest).add(0, 0.25, 0));
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-swallows-nesting");

			// Its wall gone, the nest falls with it
			BlockPos wall = nest.south();
			world.getServer().runCommand("setblock " + wall.getX() + " " + wall.getY() + " " + wall.getZ() + " air");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(3);
			check(world.getServer().computeOnServer(server -> !server.overworld().getBlockState(nest).is(SwallowNests.BLOCK)), "the nest gone with its wall");
		}

		// Now and then under the eaves of a village house, as it is built
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setSeed("vanillasugar");
			state.setWorldType(state.getNormalPresetList().getFirst());
			state.setAllowCommands(true);
		}).create()) {
			world.getConnection().waitForChunksRender();
			int nests = world.getServer().computeOnServer(server -> villageNests(server.overworld()));
			check(nests > 0, "nests in the villages around");
		}
	}

	/** The nests in the nearest villages, each generated whole. */
	private static int villageNests(ServerLevel level) {
		int total = 0;
		Set<BoundingBox> seen = new HashSet<>();
		// One each way from the world's center, a different village every time
		int[][] ways = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
		for (int i = 0; i < VILLAGES; i++) {
			BlockPos from = new BlockPos(ways[i][0] * 800, 0, ways[i][1] * 800);
			BlockPos found = level.findNearestMapStructure(StructureTags.VILLAGE, from, 100, false);
			if (found == null) {
				continue;
			}
			StructureStart start = null;
			LevelChunk origin = level.getChunk(found.getX() >> 4, found.getZ() >> 4);
			for (Holder<Structure> structure : level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getTagOrEmpty(StructureTags.VILLAGE)) {
				StructureStart candidate = origin.getStartForStructure(structure.value());
				if (candidate != null && candidate.isValid()) {
					start = candidate;
				}
			}
			BoundingBox box = start.getBoundingBox();
			if (!seen.add(box)) {
				continue;
			}
			int nests = 0;
			for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
				for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
					LevelChunk chunk = level.getChunk(cx, cz);
					for (BlockPos pos : BlockPos.betweenClosed(chunk.getPos().getMinBlockX(), box.minY(), chunk.getPos().getMinBlockZ(),
							chunk.getPos().getMaxBlockX(), box.maxY(), chunk.getPos().getMaxBlockZ())) {
						if (chunk.getBlockState(pos).is(SwallowNests.BLOCK)) {
							nests++;
						}
					}
				}
			}
			int houses = (int) start.getPieces().stream().filter(HouseFoundations::is).count();
			System.out.println("Swallows: " + nests + " nests in the village at " + box + ", " + houses + " houses, " + box.getXSpan() + " by " + box.getZSpan() + " blocks");
			total += nests;
		}
		return total;
	}

	private static String fill(BlockPos from, BlockPos to, String block) {
		return "fill " + from.getX() + " " + from.getY() + " " + from.getZ() + " " + to.getX() + " " + to.getY() + " " + to.getZ() + " " + block;
	}

	private static void look(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 at) {
		Vec3 to = at.subtract(eye);
		double yaw = Math.toDegrees(Math.atan2(-to.x, to.z));
		double pitch = -Math.toDegrees(Math.atan2(to.y, Math.hypot(to.x, to.z)));
		world.getServer().runCommand("tp @a " + eye.x + " " + (eye.y - 1.62) + " " + eye.z + " " + yaw + " " + pitch);
		world.getConnection().waitForClientboundPackets();
		context.waitTicks(3);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
