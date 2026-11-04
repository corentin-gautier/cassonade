package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import com.cassonade.Cassonade;

/** A town, a rare village grown much larger: many more houses than the village nearest to it, seen from above. */
public class TownClientGameTest implements FabricClientGameTest {
	private static final TagKey<Structure> TOWN = TagKey.create(Registries.STRUCTURE, Cassonade.id("town"));

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
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 1000");
			BoundingBox town = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				StructureStart found = nearest(level, TOWN, level.getRespawnData().pos());
				int houses = houses(found);
				// The nearest village not a town
				int villageHouses = houses(nearestVillage(level, found));
				System.out.println("Town " + found.getStructure() + " at " + found.getBoundingBox() + ": " + houses + " houses, " + found.getPieces().size()
						+ " pieces; the nearest village: " + villageHouses + " houses");
				if (houses < villageHouses * 2) {
					throw new AssertionError("Expected a town with many more houses than a village, found " + houses + " and " + villageHouses);
				}
				return found.getBoundingBox();
			});
			BlockPos center = town.getCenter();
			// Over its southern edge, looking down across it to the north, within the view distance
			world.getServer().runCommand("tp @a " + center.getX() + " " + (town.maxY() + 30) + " " + (center.getZ() + 60) + " 180 35");
			world.getConnection().waitForChunksRender();
			context.waitTicks(60);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			Screenshots.take(context, "cassonade-town");
		}
	}

	private static StructureStart nearest(ServerLevel level, TagKey<Structure> tag, BlockPos from) {
		BlockPos found = level.findNearestMapStructure(tag, from, 100, false);
		if (found == null) {
			throw new AssertionError("Expected a " + tag.location() + " in this world");
		}
		var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		return level.getChunk(found).getAllStarts().entrySet().stream().filter(entry -> structures.wrapAsHolder(entry.getKey()).is(tag)).findFirst()
				.orElseThrow().getValue();
	}

	/** The village nearest to the town, among the five around it on the villages' grid. */
	private static StructureStart nearestVillage(ServerLevel level, StructureStart town) {
		var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		BlockPos center = town.getBoundingBox().getCenter();
		StructureStart best = null;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				BlockPos found = level.findNearestMapStructure(StructureTags.VILLAGE, center.offset(dx * 544, 0, dz * 544), 2, false);
				if (found == null) {
					continue;
				}
				for (var entry : level.getChunk(found).getAllStarts().entrySet()) {
					var holder = structures.wrapAsHolder(entry.getKey());
					if (holder.is(StructureTags.VILLAGE) && !holder.is(TOWN) && (best == null || dist(best, center) > dist(entry.getValue(), center))) {
						best = entry.getValue();
					}
				}
			}
		}
		if (best == null) {
			throw new AssertionError("Expected a village around the town");
		}
		return best;
	}

	private static double dist(StructureStart start, BlockPos to) {
		return start.getBoundingBox().getCenter().distSqr(to);
	}

	private static int houses(StructureStart start) {
		return (int) start.getPieces().stream()
				.filter(piece -> piece instanceof PoolElementStructurePiece house && house.getElement().toString().contains("/houses/")).count();
	}
}
