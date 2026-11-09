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
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;
import com.cassonade.dwelling.Dwellings;
import com.cassonade.dwelling.FishermansShack;

/**
 * Lone dwellings: how many of each a fixed seed has around its center, then the nearest of each, its chest and loot,
 * and either its villager (the hermit a nitwit, the shepherd's sheep in their pen) or, when it was left, nobody and
 * cobwebs; a screenshot of each.
 */
public class LoneDwellingClientGameTest implements FabricClientGameTest {
	/** Regions of the structure set's grid counted in each direction. */
	private static final int REGIONS = 6;

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
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("time set 6000");
			for (ResourceKey<Structure> dwelling : List.of(Dwellings.HUNTERS_CABIN, Dwellings.SHEPHERDS_HUT, Dwellings.HERMITS_HOME, Dwellings.FISHERMANS_SHACK)) {
				BlockPos found = world.getServer().computeOnServer(server -> nearest(server.overworld(), dwelling));
				world.getServer().runCommand("tp @a " + found.getX() + " " + (found.getY() + 30) + " " + found.getZ());
				context.waitTicks(20);
				world.getConnection().waitForChunksRender();
				BoundingBox box = world.getServer().computeOnServer(server -> check(server.overworld(), dwelling, found));
				double x = (box.minX() + box.maxX()) / 2.0 + 0.5;
				double z = (box.minZ() + box.maxZ()) / 2.0 + 0.5;
				if (dwelling == Dwellings.FISHERMANS_SHACK) {
					// From the river, before the deck
					Vec3 view = world.getServer().computeOnServer(server -> shore(server.overworld(), found));
					world.getServer().runCommand("tp @a " + view.x + " " + view.y + " " + view.z + " facing " + x + " " + (box.minY() + 6) + " " + z);
				} else {
					// From the front, a little above, where the door is when the piece is not turned
					world.getServer().runCommand("tp @a " + (x + 10) + " " + (box.minY() + 8) + " " + (z + 12) + " facing " + x + " " + (box.minY() + 3) + " " + z);
				}
				context.waitTicks(40);
				world.getConnection().waitForChunksRender();
				context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
				Screenshots.take(context, "dwelling-" + dwelling.identifier().getPath().replace('_', '-'));
			}
		}
	}

	static Holder<Structure> structure(ServerLevel level, ResourceKey<Structure> key) {
		return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(key);
	}

	/** How many of the grid's spots around the world's center have each dwelling. */
	private static void count(ServerLevel level) {
		ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
		StringBuilder counts = new StringBuilder();
		int side = 0;
		int spots = 0;
		for (ResourceKey<Structure> key : Dwellings.ALL) {
			Holder<Structure> structure = structure(level, key);
			int present = 0;
			for (StructurePlacement placement : state.getPlacementsForStructure(structure)) {
				RandomSpreadStructurePlacement grid = (RandomSpreadStructurePlacement) placement;
				spots = 0;
				for (int x = -REGIONS; x < REGIONS; x++) {
					for (int z = -REGIONS; z < REGIONS; z++) {
						ChunkPos chunk = grid.getPotentialStructureChunk(state.getLevelSeed(), x * grid.spacing(), z * grid.spacing());
						spots++;
						if (level.structureManager().checkStructurePresence(chunk, structure.value(), placement, false) != StructureCheckResult.START_NOT_PRESENT) {
							present++;
						}
					}
				}
				side = 2 * REGIONS * grid.spacing() * 16;
			}
			counts.append(' ').append(key.identifier().getPath()).append(' ').append(present);
		}
		System.out.println("Lone dwellings in " + spots + " spots, over " + side + " x " + side + " blocks:" + counts);
	}

	static BlockPos nearest(ServerLevel level, ResourceKey<Structure> key) {
		Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(structure(level, key)), BlockPos.ZERO, 40, false);
		if (found == null) {
			throw new AssertionError("Expected a " + key.identifier() + " near the world's center");
		}
		return found.getFirst();
	}

	/** The dwelling built: its chest with its loot, and its villager, or when it was left, nobody and cobwebs. */
	private static BoundingBox check(ServerLevel level, ResourceKey<Structure> key, BlockPos found) {
		StructureStart start = level.structureManager().getStartForStructure(structure(level, key).value(), level.getChunk(found.getX() >> 4, found.getZ() >> 4));
		if (start == null || !start.isValid()) {
			throw new AssertionError("Expected a " + key.identifier() + "'s start in the chunk of " + found);
		}
		BoundingBox box = start.getPieces().getFirst().getBoundingBox();
		Identifier table = Cassonade.id("chests/" + key.identifier().getPath());
		BlockPos chest = null;
		int cobwebs = 0;
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container && container.getLootTable() != null
					&& container.getLootTable().identifier().equals(table)) {
				chest = pos.immutable();
			}
			cobwebs += level.getBlockState(pos).is(Blocks.COBWEB) ? 1 : 0;
		}
		if (chest == null) {
			throw new AssertionError("Expected a chest with " + table + " in " + box);
		}
		List<String> items = roll(level, table, chest);
		if (items.isEmpty()) {
			throw new AssertionError("Expected " + table + " to hold something");
		}
		if (key == Dwellings.FISHERMANS_SHACK) {
			checkShore(level, found);
		}
		boolean abandoned = Dwellings.abandoned(level, start.getChunkPos());
		AABB around = AABB.of(box).inflate(4);
		List<Villager> villagers = level.getEntitiesOfClass(Villager.class, around);
		List<Sheep> sheep = level.getEntitiesOfClass(Sheep.class, around);
		if (abandoned) {
			if (!villagers.isEmpty() || cobwebs == 0) {
				throw new AssertionError("Expected nobody and cobwebs in the abandoned " + key.identifier() + ", found " + villagers.size() + " villagers and " + cobwebs + " cobwebs");
			}
		} else {
			if (villagers.size() != 1) {
				throw new AssertionError("Expected one villager at the " + key.identifier() + ", found " + villagers.size());
			}
			if (key == Dwellings.HERMITS_HOME && !villagers.getFirst().getVillagerData().profession().is(VillagerProfession.NITWIT)) {
				throw new AssertionError("Expected the hermit to be a nitwit, found " + villagers.getFirst().getVillagerData());
			}
			if (key == Dwellings.SHEPHERDS_HUT && sheep.size() < 2) {
				throw new AssertionError("Expected sheep in the shepherd's pen, found " + sheep.size());
			}
		}
		System.out.println("Lone dwelling: " + key.identifier().getPath() + " at " + box + " in " + level.getBiome(chest).getRegisteredName()
				+ (abandoned ? ", abandoned, " + cobwebs + " cobwebs" : ", " + villagers.getFirst().getVillagerData() + ", " + sheep.size() + " sheep")
				+ ", its chest rolled " + items);
		return box;
	}

	/** The shack's piece: its back on the bank, the deck's end over water, its stilts down to the bed. */
	private static FishermansShack.Piece shack(ServerLevel level, BlockPos found) {
		StructureStart start = level.structureManager().getStartForStructure(structure(level, Dwellings.FISHERMANS_SHACK).value(), level.getChunk(found.getX() >> 4, found.getZ() >> 4));
		return (FishermansShack.Piece) start.getPieces().getFirst();
	}

	private static BlockPos at(FishermansShack.Piece piece, int x, int y, int z) {
		return piece.templatePosition().offset(StructureTemplate.calculateRelativePosition(piece.placeSettings(), new BlockPos(x, y, z)));
	}

	private static void checkShore(ServerLevel level, BlockPos found) {
		FishermansShack.Piece piece = shack(level, found);
		BlockPos ladder = at(piece, 3, 2, 10);
		BlockPos back = at(piece, 3, 3, 0);
		if (!level.getFluidState(ladder).is(FluidTags.WATER) || !level.getFluidState(ladder.below()).is(FluidTags.WATER)) {
			throw new AssertionError("Expected water under the shack's ladder at " + ladder + ", found " + level.getBlockState(ladder) + " over " + level.getBlockState(ladder.below()));
		}
		if (!level.getFluidState(back).isEmpty() || !level.getFluidState(back.below()).isEmpty() || !level.getBlockState(back.below()).isFaceSturdy(level, back.below(), Direction.UP)) {
			throw new AssertionError("Expected the bank under the shack's back at " + back + ", found " + level.getBlockState(back) + " over " + level.getBlockState(back.below()));
		}
		BlockPos stilt = at(piece, 3, 0, 9);
		if (!level.getBlockState(stilt).is(BlockTags.LOGS) || !level.getFluidState(stilt.below()).isEmpty() && !level.getBlockState(stilt.below()).is(BlockTags.LOGS)) {
			throw new AssertionError("Expected the deck's stilt carried down to the bed at " + stilt + ", found " + level.getBlockState(stilt.below()) + " under it");
		}
	}

	/** Out on the river, before the deck's end. */
	static Vec3 shore(ServerLevel level, BlockPos found) {
		FishermansShack.Piece piece = shack(level, found);
		BlockPos front = at(piece, 3, 6, 10);
		BlockPos back = at(piece, 3, 6, 0);
		Vec3 out = Vec3.atCenterOf(front).subtract(Vec3.atCenterOf(back)).normalize().scale(10);
		return Vec3.atCenterOf(front).add(out.x + 4, 3, out.z + 4);
	}

	private static List<String> roll(ServerLevel level, Identifier table, BlockPos at) {
		LootTable loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, table));
		List<String> items = new ArrayList<>();
		for (ItemStack stack : loot.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(at)).create(LootContextParamSets.CHEST))) {
			items.add(stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
		}
		return items;
	}
}
