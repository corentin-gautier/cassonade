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
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;
import com.cassonade.treasure.PirateBooty;

/**
 * Pirate's booty: piles of barrels with their loot table on lush caves' floors, found around a fixed seed's lush
 * caves, few of them for the caves' size, and a screenshot of one. The structure search finds one, as a shipwreck's
 * pirate map does, and the shipwrecks' map chests now and then hold such a map.
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
			List<BlockPos> hoards = world.getServer().computeOnServer(server -> hoards(server.overworld()));
			if (hoards.isEmpty()) {
				throw new AssertionError("Expected at least one pirate's booty around the lush caves");
			}
			world.getServer().runOnServer(server -> located(server.overworld(), hoards.getFirst()));
			view(context, world, hoards.getFirst());
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "pirate-booty");
		}
	}

	/** The hoards around the lush caves nearest to the world's center, one position per pile. */
	static List<BlockPos> hoards(ServerLevel level) {
		Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome -> biome.is(Biomes.LUSH_CAVES), BlockPos.ZERO, 6400, 32, 64);
		if (found == null) {
			throw new AssertionError("Expected lush caves in this world");
		}
		int cx = found.getFirst().getX() >> 4;
		int cz = found.getFirst().getZ() >> 4;
		Structure booty = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(ResourceKey.create(Registries.STRUCTURE, Cassonade.id("pirate_booty")));
		List<BlockPos> barrels = new ArrayList<>();
		int starts = 0;
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
				int inChunk = 0;
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof BarrelBlockEntity barrel && PirateBooty.LOOT.equals(barrel.getLootTable())) {
						barrels.add(barrel.getBlockPos());
						inChunk++;
					}
				}
				// Every booty a map can lead to has its barrels, as vanilla's buried treasure always has its chest
				StructureStart start = chunk.getStartForStructure(booty);
				if (start != null && start.isValid()) {
					starts++;
					if (inChunk == 0) {
						throw new AssertionError("Expected the booty's barrels in its chunk " + chunk.getPos());
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
		System.out.println("Pirate's booty: " + starts + " booties, " + piles.size() + " hoards, " + barrels.size() + " barrels, in " + lush + " chunks of lush caves out of "
				+ (2 * RADIUS + 1) * (2 * RADIUS + 1) + ": " + piles);
		return piles;
	}

	/** The structure search from a hoard finds a booty with its barrels, and pirate maps come out of map chests there. */
	private static void located(ServerLevel level, BlockPos from) {
		BlockPos found = level.findNearestMapStructure(PirateBooty.MAP_TAG, from, 50, false);
		if (found == null) {
			throw new AssertionError("Expected the structure search to find the pirate's booty near " + from);
		}
		LevelChunk chunk = level.getChunk(found.getX() >> 4, found.getZ() >> 4);
		long barrels = chunk.getBlockEntities().values().stream()
				.filter(entity -> entity instanceof BarrelBlockEntity barrel && PirateBooty.LOOT.equals(barrel.getLootTable())).count();
		if (barrels == 0) {
			throw new AssertionError("Expected the booty's barrels where the search found it, in the chunk of " + found);
		}
		// One map chest in 4 holds a pirate map besides vanilla's, when a booty is near enough
		LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SHIPWRECK_MAP);
		int maps = 0;
		for (int i = 0; i < 40; i++) {
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(from)).create(LootContextParamSets.CHEST);
			for (ItemStack stack : table.getRandomItems(params)) {
				if (stack.get(DataComponents.ITEM_NAME) != null && stack.get(DataComponents.ITEM_NAME).getContents() instanceof TranslatableContents name
						&& name.getKey().equals("filled_map.cassonade.pirate_map")) {
					maps++;
				}
			}
		}
		System.out.println("Pirate's booty: found by the search at " + found + " with " + barrels + " barrels, " + maps + " pirate maps in 40 map chests");
		// About 10 expected
		if (maps < 3) {
			throw new AssertionError("Expected pirate maps in the shipwrecks' map chests, found " + maps + " in 40");
		}
	}

	/** The camera somewhere open in the cave a few blocks from the pile, looking at it, by the light of night vision. */
	static void view(ClientGameTestContext context, TestSingleplayerContext world, BlockPos hoard) {
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
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
	}
}
