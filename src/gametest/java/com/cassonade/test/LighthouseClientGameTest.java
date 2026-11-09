package com.cassonade.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;
import com.cassonade.client.lighthouse.LighthouseLamps;
import com.cassonade.lighthouse.Lighthouses;

/**
 * Lighthouses: how many a fixed seed has on its coasts, the nearest one built with its sea lantern where the server
 * says the lamp is, its keeper, its chest and the lookout's barrel; a screenshot by day, then one at night from far out, where the client
 * shows the lamp the server sent.
 */
public class LighthouseClientGameTest implements FabricClientGameTest {
	/** Regions of the structure set's grid counted in each direction. */
	private static final int REGIONS = 8;
	/** How far out the night view stands. */
	private static final int FAR = 400;

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
			world.getServer().runOnServer(server -> count(server.overworld()));
			BlockPos found = world.getServer().computeOnServer(server -> nearest(server.overworld()));
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("tp @a " + found.getX() + " " + (found.getY() + 40) + " " + found.getZ());
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			BoundingBox box = world.getServer().computeOnServer(server -> check(server.overworld(), found));
			BlockPos lamp = world.getServer().computeOnServer(server -> Lighthouses.lamp(server.overworld(), server.overworld().structureManager().getStartForStructure(structure(server.overworld()).value(), server.overworld().getChunk(found.getX() >> 4, found.getZ() >> 4))).orElseThrow());

			// By day, from the shore, a little above
			world.getServer().runCommand("tp @a " + (lamp.getX() + 24.5) + " " + (box.minY() + 12) + " " + (lamp.getZ() + 18.5) + " facing " + (lamp.getX() + 0.5) + " "
					+ (box.minY() + 12) + " " + (lamp.getZ() + 0.5));
			context.waitTicks(40);
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			Screenshots.take(context, "lighthouse");

			// At night, far out: the lamp the server sent, past the terrain drawn
			Vec3 out = world.getServer().computeOnServer(server -> seaward(server.overworld(), lamp));
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("tp @a " + out.x + " " + out.y + " " + out.z + " facing " + (lamp.getX() + 0.5) + " " + (lamp.getY() + 0.5) + " " + (lamp.getZ() + 0.5));
			context.waitFor(client -> LighthouseLamps.lamps().contains(lamp), 400);
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			Screenshots.take(context, "lighthouse-night-far");
						// The beam's whole turn, so one frame or two catch its pulse
			for (int i = 0; i < 6; i++) {
				context.waitTicks(20);
				Screenshots.take(context, "lighthouse-night-far-" + i);
			}
		}
	}

	private static Holder<Structure> structure(ServerLevel level) {
		return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(Lighthouses.STRUCTURE);
	}

	/** How many of the grid's spots around the world's center have a lighthouse. */
	private static void count(ServerLevel level) {
		ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
		Holder<Structure> structure = structure(level);
		int present = 0;
		int spots = 0;
		for (StructurePlacement placement : state.getPlacementsForStructure(structure)) {
			RandomSpreadStructurePlacement grid = (RandomSpreadStructurePlacement) placement;
			for (int x = -REGIONS; x < REGIONS; x++) {
				for (int z = -REGIONS; z < REGIONS; z++) {
					ChunkPos chunk = grid.getPotentialStructureChunk(state.getLevelSeed(), x * grid.spacing(), z * grid.spacing());
					spots++;
					if (level.structureManager().checkStructurePresence(chunk, structure.value(), placement, false) != StructureCheckResult.START_NOT_PRESENT) {
						present++;
					}
				}
			}
			int side = 2 * REGIONS * grid.spacing() * 16;
			System.out.println("Lighthouses: " + present + " in " + spots + " spots, over " + side + " x " + side + " blocks");
		}
		if (present == 0) {
			throw new AssertionError("Expected lighthouses on this world's coasts");
		}
	}

	private static BlockPos nearest(ServerLevel level) {
		Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(structure(level)), BlockPos.ZERO, 100, false);
		if (found == null) {
			throw new AssertionError("Expected a lighthouse near the world's center");
		}
		return found.getFirst();
	}

	/** The lighthouse built: its sea lantern where the server puts the lamp, a keeper and a chest with its loot. */
	private static BoundingBox check(ServerLevel level, BlockPos found) {
		StructureStart start = level.structureManager().getStartForStructure(structure(level).value(), level.getChunk(found.getX() >> 4, found.getZ() >> 4));
		if (start == null || !start.isValid()) {
			throw new AssertionError("Expected a lighthouse's start in the chunk of " + found);
		}
		BoundingBox box = start.getPieces().getFirst().getBoundingBox();
		// On the ground, not floating over the sea
		for (BlockPos corner : List.of(new BlockPos(box.minX(), box.minY() - 1, box.minZ()), new BlockPos(box.maxX(), box.minY() - 1, box.minZ()),
				new BlockPos(box.minX(), box.minY() - 1, box.maxZ()), new BlockPos(box.maxX(), box.minY() - 1, box.maxZ()))) {
			if (!level.getFluidState(corner).isEmpty()) {
				throw new AssertionError("Expected ground under the lighthouse's corner at " + corner + ", found " + level.getBlockState(corner));
			}
		}
		BlockPos lamp = Lighthouses.lamp(level, start).orElseThrow(() -> new AssertionError("Expected a light in the lighthouse's template"));
		if (level.getBlockState(lamp).getLightEmission() == 0 || lamp.getY() < box.maxY() - 10) {
			throw new AssertionError("Expected the lamp at the top of the tower, at " + lamp + ", found " + level.getBlockState(lamp));
		}
		List<Villager> keepers = level.getEntitiesOfClass(Villager.class, AABB.of(box).inflate(4));
		if (keepers.isEmpty()) {
			throw new AssertionError("Expected the lighthouse keeper in " + box);
		}
		// Every container with loot, by its table: the cottage's chest and the lookout's barrel
		Map<Identifier, BlockPos> containers = new HashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container && container.getLootTable() != null) {
				containers.put(container.getLootTable().identifier(), pos.immutable());
			}
		}
		Identifier chest = Cassonade.id("chests/lighthouse");
		Identifier lookout = Cassonade.id("chests/lighthouse_lookout");
		if (!containers.keySet().equals(Set.of(chest, lookout))) {
			throw new AssertionError("Expected the cottage's chest and the lookout's barrel with their loot, found " + containers);
		}
		if (containers.get(lookout).getY() < box.maxY() - 10 || !level.getBlockState(containers.get(lookout)).is(Blocks.BARREL)) {
			throw new AssertionError("Expected the lookout's barrel at the top of the tower, found it at " + containers.get(lookout));
		}
		if (level.getPoiManager().existsAtPosition(PoiTypes.FISHERMAN, containers.get(lookout))) {
			throw new AssertionError("Expected the lookout's barrel not to be a workstation");
		}
		List<String> items = roll(level, chest, lamp);
		if (items.isEmpty()) {
			throw new AssertionError("Expected the lighthouse's chest to hold something");
		}
		// Not always a spyglass up there, but often
		int spyglasses = 0;
		int naturalist = 0;
		for (int i = 0; i < 400; i++) {
			for (String item : roll(level, lookout, lamp)) {
				spyglasses += item.endsWith("minecraft:spyglass") ? 1 : 0;
				naturalist += item.endsWith("cassonade:naturalist_spyglass") ? 1 : 0;
			}
		}
		if (spyglasses + naturalist < 160 || spyglasses + naturalist > 260 || naturalist == 0 || naturalist > 40) {
			throw new AssertionError("Expected a spyglass in about half the lookout's barrels, a naturalist's rarely: " + spyglasses + " and " + naturalist + " in 400");
		}
		System.out.println("Lighthouse: at " + box + " in " + level.getBiome(lamp).getRegisteredName() + ", lamp " + level.getBlockState(lamp) + ", keeper "
				+ keepers.getFirst().getVillagerData() + ", a chest rolled " + items + ", the lookout's barrels " + spyglasses + " spyglasses and " + naturalist
				+ " naturalist's in 400");
		return box;
	}

	private static List<String> roll(ServerLevel level, Identifier table, BlockPos at) {
		LootTable loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, table));
		List<String> items = new ArrayList<>();
		for (ItemStack stack : loot.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(at)).create(LootContextParamSets.CHEST))) {
			items.add(stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
		}
		return items;
	}

	/** Far from the lamp over the sea if there is one that way, at the lamp's height, so nothing but water lies between. */
	private static Vec3 seaward(ServerLevel level, BlockPos lamp) {
		int bestWater = -1;
		Vec3 best = null;
		for (int i = 0; i < 16; i++) {
			double angle = i * Math.PI / 8;
			int water = 0;
			for (int step = 32; step <= FAR; step += 32) {
				BlockPos column = BlockPos.containing(lamp.getX() + Math.cos(angle) * step, level.getSeaLevel() - 1, lamp.getZ() + Math.sin(angle) * step);
				water += level.getChunkSource().getGenerator().getBaseHeight(column.getX(), column.getZ(),
						Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState()) < level.getSeaLevel() - 2 ? 1 : 0;
			}
			if (water > bestWater) {
				bestWater = water;
				best = new Vec3(lamp.getX() + 0.5 + Math.cos(angle) * FAR, lamp.getY() + 0.5, lamp.getZ() + 0.5 + Math.sin(angle) * FAR);
			}
		}
		return best;
	}
}
