package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

import com.cassonade.tweaks.Markets;
import com.cassonade.tweaks.Taverns;

/**
 * A town's meeting point is its market square: a bell among stalls whose barrels hold goods and make no fishermen, a
 * wandering trader sent to the bell settling at the bare stall; and the villages' lamp posts, lanterns on posts, dark
 * in a zombie village. Screenshots of the market by day, of the trader at its stall, and of the lamp posts at night.
 */
public class MarketClientGameTest implements FabricClientGameTest {
	private static final List<String> LAMPS = List.of("plains/plains_lamp_1", "desert/desert_lamp_1", "savanna/savanna_lamp_post_01",
			"taiga/taiga_lamp_post_1", "snowy/snowy_lamp_post_01");

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
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set 1000");
			BoundingBox square = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				StructureStart town = TownClientGameTest.town(level);
				StructurePiece first = town.getPieces().getFirst();
				if (!Markets.is(first)) {
					throw new AssertionError("Expected the town's meeting point to be a market, found " + first);
				}
				BoundingBox box = first.getBoundingBox();
				// Generated whole, so its barrels and bell stand
				for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
					for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
						level.getChunk(x, z);
					}
				}
				return box;
			});
			context.waitTicks(20);
			BlockPos[] found = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				BlockPos bell = null;
				int barrels = 0;
				for (BlockPos pos : BlockPos.betweenClosed(square.minX(), square.minY(), square.minZ(), square.maxX(), square.maxY(), square.maxZ())) {
					if (level.getBlockState(pos).is(Blocks.BELL)) {
						bell = pos.immutable();
					} else if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel && barrel.getLootTable() != null) {
						barrels++;
						if (level.getPoiManager().existsAtPosition(PoiTypes.FISHERMAN, pos)) {
							throw new AssertionError("Expected the market's barrels to make no fishermen, found one at " + pos);
						}
					}
				}
				System.out.println("Market " + square + ": bell at " + bell + ", " + barrels + " barrels with goods");
				if (bell == null || barrels < 4) {
					throw new AssertionError("Expected a bell and barrels of goods in the market, found " + bell + " and " + barrels);
				}
				BlockPos spot = Markets.traderSpot(level, bell);
				if (spot == null || !square.isInside(spot) || !level.getBlockState(spot).isAir() || !level.getBlockState(spot.above()).isAir()) {
					throw new AssertionError("Expected room for a trader at the bare stall, found " + spot);
				}
				return new BlockPos[] { bell, spot };
			});
			BlockPos bell = found[0];
			BlockPos spot = found[1];

			// The market from above the street, then a trader sent to its bell, as the game sends them, walking to its stall
			BlockPos center = square.getCenter();
			world.getServer().runCommand("tp @a " + (center.getX() + 0.5) + " " + (square.minY() + 12) + " " + (square.maxZ() + 12) + " facing "
					+ center.getX() + " " + square.minY() + " " + center.getZ());
			world.getConnection().waitForChunksRender();
			context.waitTicks(40);
			clearChat(context);
			Screenshots.take(context, "market");

			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				WanderingTrader trader = EntityTypes.WANDERING_TRADER.spawn(level, bell.offset(0, -1, -4), EntitySpawnReason.EVENT);
				if (trader == null) {
					throw new AssertionError("Expected a wandering trader to spawn by the bell");
				}
				trader.setWanderTarget(bell);
				if (!Markets.settle(level, trader, bell)) {
					throw new AssertionError("Expected the trader to settle in the market");
				}
			});
			boolean settled = false;
			for (int second = 0; second < 60 && !settled; second++) {
				context.waitTicks(20);
				settled = world.getServer().computeOnServer(server -> traderNear(server.overworld(), spot, 1.5));
			}
			if (!settled) {
				throw new AssertionError("Expected the trader to reach its stall at " + spot + " within 60 seconds");
			}
			// And stay there
			context.waitTicks(400);
			if (!world.getServer().computeOnServer(server -> traderNear(server.overworld(), spot, 2.5))) {
				throw new AssertionError("Expected the trader to stay at its stall at " + spot);
			}
			// From above the stall's corner, over its awning's edge
			world.getServer().runCommand("tp @a " + (spot.getX() + 5.5) + " " + (spot.getY() + 4) + " " + (spot.getZ() + 5.5) + " facing "
					+ (spot.getX() + 0.5) + " " + (spot.getY() + 0.8) + " " + (spot.getZ() + 0.5));
			context.waitTicks(20);
			clearChat(context);
			Screenshots.take(context, "market-trader");

			lampPosts(context, world);
		}
	}

	private static boolean traderNear(ServerLevel level, BlockPos spot, double range) {
		return !level.getEntitiesOfClass(WanderingTrader.class, new AABB(spot).inflate(range, 1, range)).isEmpty();
	}

	/** Each biome's lamp post in a row at night, then the plains' in a zombie village, dark. */
	private static void lampPosts(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		int x0 = 2000;
		int z = 2000;
		world.getServer().runCommand("forceload add " + x0 + " " + z + " " + (x0 + 6 * LAMPS.size()) + " " + (z + 4));
		int ground = world.getServer().computeOnServer(server -> server.overworld().getHeight(Heightmap.Types.WORLD_SURFACE, x0, z));
		world.getServer().runCommand("fill " + (x0 - 4) + " " + (ground - 1) + " " + (z - 4) + " " + (x0 + 6 * LAMPS.size() + 4) + " " + (ground - 1) + " " + (z + 8)
				+ " minecraft:dirt_path");
		world.getServer().runCommand("fill " + (x0 - 4) + " " + ground + " " + (z - 4) + " " + (x0 + 6 * LAMPS.size() + 4) + " " + (ground + 8) + " " + (z + 8)
				+ " minecraft:air");
		for (int i = 0; i < LAMPS.size(); i++) {
			world.getServer().runCommand("place template minecraft:village/" + LAMPS.get(i) + " " + (x0 + 6 * i) + " " + ground + " " + z);
		}
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			for (int i = 0; i < LAMPS.size(); i++) {
				int lanterns = 0;
				for (BlockPos pos : BlockPos.betweenClosed(x0 + 6 * i, ground, z, x0 + 6 * i + 3, ground + 5, z + 3)) {
					lanterns += level.getBlockState(pos).is(Blocks.LANTERN) ? 1 : 0;
				}
				System.out.println("Lamp post " + LAMPS.get(i) + ": " + lanterns + " lanterns");
				if (lanterns == 0) {
					throw new AssertionError("Expected a lantern on the lamp post " + LAMPS.get(i));
				}
			}
		});
		world.getServer().runCommand("tp @a " + (x0 + 3 * LAMPS.size()) + " " + (ground + 2) + " " + (z + 10) + " facing " + (x0 + 3 * LAMPS.size()) + " "
				+ (ground + 2) + " " + z);
		world.getConnection().waitForChunksRender();
		context.waitTicks(40);
		clearChat(context);
		Screenshots.take(context, "lamp-posts");

		// As a zombie village's generation does: the lanterns come down
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			BoundingBox box = new BoundingBox(x0, ground, z, x0 + 3, ground + 5, z + 3);
			Taverns.abandon(level, box);
			for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
				if (level.getBlockState(pos).is(Blocks.LANTERN)) {
					throw new AssertionError("Expected an abandoned lamp post to be dark, found a lantern at " + pos);
				}
			}
		});
	}

	private static void clearChat(ClientGameTestContext context) {
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
	}
}
