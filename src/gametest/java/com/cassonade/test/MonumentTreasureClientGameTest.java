package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

import com.cassonade.treasure.MonumentTreasure;

/** A generated ocean monument has barrels of treasure in its treasure room, next to the gold blocks. */
public class MonumentTreasureClientGameTest implements FabricClientGameTest {
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
			world.getServer().runCommand("time set 6000");
			BlockPos monument = world.getServer().computeOnServer(server ->
					server.overworld().findNearestMapStructure(StructureTags.ON_OCEAN_MONUMENT_MAPS, server.overworld().getRespawnData().pos(), 100, false));
			world.getServer().runCommand("tp @a " + monument.getX() + " 70 " + monument.getZ());
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(200);
			BlockPos[] found = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(BuiltinStructures.OCEAN_MONUMENT);
				BoundingBox box = level.getChunk(monument).getStartForStructure(structure).getBoundingBox();
				BlockPos first = null;
				BlockPos gold1 = null;
				int barrels = 0;
				int gold = 0;
				for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
					if (level.getBlockState(pos).is(Blocks.GOLD_BLOCK)) {
						gold++;
						gold1 = gold1 == null ? pos.immutable() : gold1;
					} else if (level.getBlockState(pos).is(Blocks.BARREL) && level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) {
						if (!MonumentTreasure.LOOT.equals(container.getLootTable())) {
							throw new AssertionError("Expected the monument's loot in the barrel at " + pos + ", found " + container.getLootTable());
						}
						barrels++;
						first = first == null ? pos.immutable() : first;
					}
				}
				System.out.println("Monument treasure: " + barrels + " barrels and " + gold + " gold blocks");
				if (gold != 8) {
					throw new AssertionError("Expected vanilla's 8 gold blocks in the treasure room, found " + gold);
				}
				if (barrels < 1 || barrels > 4) {
					throw new AssertionError("Expected 1 to 4 barrels in the treasure room's corners, found " + barrels);
				}
				return new BlockPos[] {first, gold1};
			});
			// From between the gold blocks and the corner, looking at the barrel
			BlockPos barrel = found[0];
			double dx = found[1].getX() - barrel.getX();
			double dz = found[1].getZ() - barrel.getZ();
			double length = Math.sqrt(dx * dx + dz * dz);
			world.getServer().runCommand("tp @a " + (barrel.getX() + 0.5 + dx / length * 4) + " " + (barrel.getY() + 3) + " " + (barrel.getZ() + 0.5 + dz / length * 4) + " facing "
					+ (barrel.getX() + 0.5) + " " + (barrel.getY() + 0.5) + " " + (barrel.getZ() + 0.5));
			world.getServer().runCommand("effect give @a night_vision infinite 0 true");
			world.getConnection().waitForChunksRender();
			context.waitTicks(40);
			Screenshots.take(context, "cassonade-monument-treasure");
		}
	}
}
