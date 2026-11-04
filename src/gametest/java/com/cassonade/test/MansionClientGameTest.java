package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

/** A generated woodland mansion is built of stone bricks rather than cobblestone, and has lanterns and candles in its rooms on every floor, no torch and no empty chest. */
public class MansionClientGameTest implements FabricClientGameTest {
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
			world.getServer().runCommand("time set 18000");
			BlockPos mansion = world.getServer().computeOnServer(server ->
					server.overworld().findNearestMapStructure(StructureTags.ON_WOODLAND_MANSION_MAPS, server.overworld().getRespawnData().pos(), 100, false));
			world.getServer().runCommand("tp @a " + mansion.getX() + " 90 " + mansion.getZ());
			world.getServer().runCommand("execute as @p at @s run loot spawn ~ ~ ~ loot minecraft:chests/woodland_mansion");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(300);
			BlockPos lantern = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				BlockPos center = server.getPlayerList().getPlayers().getFirst().blockPosition();
				BlockPos first = null;
				int lanterns = 0;
				int candles = 0;
				int chests = 0;
				int barrels = 0;
				int templates = 0;
				int bricks = 0;
				for (BlockPos pos : BlockPos.betweenClosed(center.offset(-60, -40, -60), center.offset(60, 20, 60))) {
					if (level.getBlockState(pos).is(Blocks.LANTERN)) {
						lanterns++;
						first = first == null ? pos.immutable() : first;
					} else if (level.getBlockState(pos).is(Blocks.COBBLESTONE) || level.getBlockState(pos).is(Blocks.COBBLESTONE_STAIRS)
							|| level.getBlockState(pos).is(Blocks.COBBLESTONE_SLAB) || level.getBlockState(pos).is(Blocks.COBBLESTONE_WALL)) {
						throw new AssertionError("Expected stone bricks instead of cobblestone, found " + level.getBlockState(pos) + " at " + pos);
					} else if (level.getBlockState(pos).is(Blocks.STONE_BRICKS) || level.getBlockState(pos).is(Blocks.MOSSY_STONE_BRICKS)) {
						bricks++;
					} else if (level.getBlockState(pos).is(Blocks.CANDLE)) {
						candles++;
					} else if (level.getBlockState(pos).is(Blocks.TORCH) || level.getBlockState(pos).is(Blocks.WALL_TORCH)) {
						throw new AssertionError("Expected no torch, found one at " + pos);
					} else if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest) {
						if (level.getBlockState(pos).is(Blocks.BARREL)) {
							barrels++;
							if (!level.getBlockState(pos.below()).isSolidRender()) {
								throw new AssertionError("Expected a barrel on the floor, not floating at " + pos);
							}
							if (Direction.Plane.HORIZONTAL.stream().noneMatch(side -> level.getBlockState(pos.relative(side).above(2)).isSolidRender())) {
								throw new AssertionError("Expected a barrel against a wall at " + pos);
							}
							if (chest.getLootTable().identifier().getPath().endsWith("_template")) {
								templates++;
							}
						} else {
							chests++;
						}
						if (chest.getLootTable() == null && chest.isEmpty()) {
							throw new AssertionError("Expected no empty chest, found one at " + pos);
						}
					}
				}
				System.out.println("Mansion: " + lanterns + " lanterns, " + candles + " candles, " + chests + " chests, " + barrels + " barrels, " + templates + " templates, " + bricks + " stone bricks");
				if (bricks < 500) {
					throw new AssertionError("Expected stone bricks in the mansion's walls, found " + bricks);
				}
				if (barrels == 0 || templates > 2) {
					throw new AssertionError("Expected barrels and at most 2 templates, found " + barrels + " and " + templates);
				}
				if (lanterns < 20) {
					throw new AssertionError("Expected lanterns in the rooms, found " + lanterns + " and " + candles + " candles");
				}
				int light = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, first.below());
				if (light < 10) {
					throw new AssertionError("Expected the lantern to light its room, found light " + light);
				}
				return first;
			});
			world.getServer().runCommand("tp @a " + lantern.getX() + " " + (lantern.getY() - 1) + " " + (lantern.getZ() + 3) + " 180 -10");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(60);
			Screenshots.take(context, "cassonade-mansion-room");
			world.getServer().runCommand("tp @a " + (lantern.getX() + 3) + " " + (lantern.getY() - 1) + " " + lantern.getZ() + " 90 -10");
			context.waitTicks(20);
			Screenshots.take(context, "cassonade-mansion-room-east");
			world.getServer().runCommand("tp @a " + (lantern.getX() - 3) + " " + (lantern.getY() - 1) + " " + lantern.getZ() + " -90 -10");
			context.waitTicks(20);
			Screenshots.take(context, "cassonade-mansion-room-west");
			// The front by day, its ground floor's walls and the foundation in stone bricks
			world.getServer().runCommand("time set 1000");
			BoundingBox box = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(BuiltinStructures.WOODLAND_MANSION);
				return level.getChunk(mansion).getStartForStructure(structure).getBoundingBox();
			});
			world.getServer().runCommand("tp @a " + box.getCenter().getX() + " " + (box.minY() + 10) + " " + (box.maxZ() + 24) + " facing " + box.getCenter().getX() + " "
					+ (box.minY() + 6) + " " + box.getCenter().getZ());
			world.getConnection().waitForChunksRender();
			context.waitTicks(40);
			Screenshots.take(context, "cassonade-mansion-front");
		}
	}
}
