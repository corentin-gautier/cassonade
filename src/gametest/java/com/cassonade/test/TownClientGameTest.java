package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import com.cassonade.tweaks.Towns;

/** A town, a rare village grown larger and denser: many more houses than the village nearest to it, closer together, none over another, seen from above. */
public class TownClientGameTest implements FabricClientGameTest {
	private static final TagKey<Structure> TOWN = Towns.TAG;

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
			BlockPos heart = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				StructureStart found = town(level);
				List<BoundingBox> houses = houses(found);
				// The nearest village not a town
				List<BoundingBox> villageHouses = houses(nearestVillage(level, found));
				double spacing = spacing(houses);
				double villageSpacing = spacing(villageHouses);
				System.out.printf("Town %s at %s: %d houses %.1f blocks apart; the nearest village: %d houses %.1f blocks apart%n", found.getStructure(),
						found.getBoundingBox(), houses.size(), spacing, villageHouses.size(), villageSpacing);
				if (houses.size() < villageHouses.size() * 2) {
					throw new AssertionError("Expected a town with many more houses than a village, found " + houses.size() + " and " + villageHouses.size());
				}
				if (spacing >= villageSpacing) {
					throw new AssertionError("Expected the town's houses closer together than the village's, found " + spacing + " and " + villageSpacing);
				}
				for (int i = 0; i < houses.size(); i++) {
					for (int j = i + 1; j < houses.size(); j++) {
						if (houses.get(i).intersects(houses.get(j))) {
							throw new AssertionError("Expected no house over another, found " + houses.get(i) + " and " + houses.get(j));
						}
					}
				}
				// Its meeting point, where the streets start
				return found.getPieces().getFirst().getBoundingBox().getCenter();
			});
			// Behind its meeting point, looking down the streets beyond it, within the view distance
			world.getServer().runCommand("tp @a " + heart.getX() + " " + (heart.getY() + 35) + " " + (heart.getZ() + 25) + " 180 45");
			world.getConnection().waitForChunksRender();
			context.waitTicks(60);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			Screenshots.take(context, "cassonade-town");
		}
	}

	/** The town nearest the spawn, the same in every run of this seed. */
	static StructureStart town(ServerLevel level) {
		return nearest(level, TOWN, level.getRespawnData().pos());
	}

	/**
	 * Of the towns around the spawn, the one that shows best from above: the most houses on the flattest ground, its
	 * houses' floors the least apart in height.
	 */
	static StructureStart showcaseTown(ServerLevel level) {
		var generator = level.getChunkSource().getGeneratorState();
		var towns = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(TOWN);
		RandomSpreadStructurePlacement spread = towns.stream().flatMap(town -> generator.getPlacementsForStructure(town).stream())
				.filter(RandomSpreadStructurePlacement.class::isInstance).map(RandomSpreadStructurePlacement.class::cast).findFirst().orElseThrow();
		ChunkPos spawn = ChunkPos.containing(level.getRespawnData().pos());
		StructureStart best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (int dx = -8; dx <= 8; dx++) {
			for (int dz = -8; dz <= 8; dz++) {
				ChunkPos spot = spread.getPotentialStructureChunk(generator.getLevelSeed(), Math.floorDiv(spawn.x(), spread.spacing()) * spread.spacing() + dx * spread.spacing(),
						Math.floorDiv(spawn.z(), spread.spacing()) * spread.spacing() + dz * spread.spacing());
				for (StructureStart start : level.getChunk(spot.x(), spot.z(), ChunkStatus.STRUCTURE_STARTS).getAllStarts().values()) {
					if (!start.isValid() || towns.stream().noneMatch(town -> town.value() == start.getStructure())) {
						continue;
					}
					List<BoundingBox> houses = houses(start);
					double mean = houses.stream().mapToInt(BoundingBox::minY).average().orElse(0);
					double spreadY = Math.sqrt(houses.stream().mapToDouble(house -> (house.minY() - mean) * (house.minY() - mean)).average().orElse(0));
					double score = houses.size() - 6 * spreadY;
					if (score > bestScore) {
						bestScore = score;
						best = start;
					}
				}
			}
		}
		if (best == null) {
			throw new AssertionError("Expected a town around the spawn");
		}
		return best;
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

	private static List<BoundingBox> houses(StructureStart start) {
		return start.getPieces().stream()
				.filter(piece -> piece instanceof PoolElementStructurePiece house && house.getElement().toString().contains("/houses/"))
				.map(piece -> piece.getBoundingBox()).toList();
	}

	/** How far, on average, from a house's middle to the nearest other house's. */
	private static double spacing(List<BoundingBox> houses) {
		return houses.stream().mapToDouble(house -> houses.stream().filter(other -> other != house)
				.mapToDouble(other -> Math.sqrt(house.getCenter().distSqr(other.getCenter()))).min().orElse(0)).average().orElse(0);
	}
}
