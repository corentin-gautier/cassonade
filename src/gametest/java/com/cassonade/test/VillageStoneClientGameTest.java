package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** A generated village's houses are built of stone bricks, some mossy or cracked, with no cobblestone left. */
public class VillageStoneClientGameTest implements FabricClientGameTest {
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
			BoundingBox village = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				BlockPos found = level.findNearestMapStructure(StructureTags.VILLAGE, level.getRespawnData().pos(), 100, false);
				if (found == null) {
					throw new AssertionError("Expected a village in this world");
				}
				var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
				StructureStart start = level.getChunk(found).getAllStarts().entrySet().stream()
						.filter(entry -> structures.wrapAsHolder(entry.getKey()).is(StructureTags.VILLAGE)).findFirst().orElseThrow().getValue();
				BoundingBox box = start.getBoundingBox();
				int bricks = 0;
				for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
					// Only the village's own pieces: a dungeon's mossy cobblestone may lie in the box, under the houses
					if (start.getPieces().stream().noneMatch(piece -> piece.getBoundingBox().isInside(pos))) {
						continue;
					}
					BlockState state = level.getBlockState(pos);
					if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBBLESTONE_STAIRS) || state.is(Blocks.COBBLESTONE_SLAB) || state.is(Blocks.COBBLESTONE_WALL)
							|| state.is(Blocks.MOSSY_COBBLESTONE)) {
						throw new AssertionError("Expected stone bricks instead of cobblestone, found " + state + " at " + pos);
					}
					if (state.is(Blocks.STONE_BRICKS) || state.is(Blocks.MOSSY_STONE_BRICKS) || state.is(Blocks.CRACKED_STONE_BRICKS)) {
						bricks++;
					}
				}
				System.out.println("Village at " + box + ": " + bricks + " stone bricks");
				if (bricks < 50) {
					throw new AssertionError("Expected stone bricks in the village's houses, found " + bricks);
				}
				return box;
			});
			BlockPos center = village.getCenter();
			world.getServer().runCommand("tp @a " + (center.getX() - 30) + " " + (village.minY() + 22) + " " + (center.getZ() - 30) + " facing " + center.getX() + " "
					+ (village.minY() + 4) + " " + center.getZ());
			world.getConnection().waitForChunksRender();
			context.waitTicks(40);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			Screenshots.take(context, "cassonade-village-stone");
		}
	}
}
