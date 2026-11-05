package com.cassonade.test;

import java.util.Comparator;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import com.cassonade.tweaks.TavernGatherings;

/**
 * In the evening, a village's villagers gather in its tavern rather than at the bell while its fire burns, and stay at
 * the bell once it is out: a real village of a fixed seed, at the meeting hour, with a screenshot of the gathering.
 */
public class TavernEveningClientGameTest implements FabricClientGameTest {
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
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 1000");
			context.runOnClient(client -> client.player.connection.sendCommand("dev goto tavern"));
			context.waitTicks(20);
			world.getServer().runCommand("gamemode spectator @a");
			world.getConnection().waitForChunksRender();
			// Time for the villagers to find the bell, the meeting point they keep
			BlockPos bell = null;
			for (int second = 0; second < 60 && bell == null; second++) {
				context.waitTicks(20);
				bell = world.getServer().computeOnServer(server -> villagers(server).stream()
						.flatMap(villager -> villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT).stream()).map(GlobalPos::pos).findFirst().orElse(null));
			}
			if (bell == null) {
				throw new AssertionError("Expected the village's villagers to know their bell within 60 seconds");
			}
			BlockPos meetingPoint = bell;
			TavernGatherings.Hall hall = world.getServer().computeOnServer(server -> TavernGatherings.find(server.overworld(), meetingPoint));
			if (hall == null) {
				throw new AssertionError("Expected a tavern with its fire lit in the village of the bell at " + meetingPoint);
			}
			System.out.println("Tavern evening: bell at " + meetingPoint + ", tavern fire at " + hall.fire() + ", " + hall.floor().size() + " spots of floor");

			// The meeting hour: the tavern fills up
			world.getServer().runCommand("time set 9500");
			int inside = 0;
			for (int second = 0; second < 120 && inside < 3; second++) {
				context.waitTicks(20);
				inside = world.getServer().computeOnServer(server -> inside(server, hall));
			}
			int total = world.getServer().computeOnServer(server -> villagers(server).size());
			System.out.println("Tavern evening: " + inside + " of " + total + " villagers in the tavern");
			if (inside < 3) {
				throw new AssertionError("Expected at least 3 villagers in the tavern at the meeting hour, found " + inside + " of " + total);
			}
			context.waitTicks(200);
			BlockPos fire = hall.fire();
			// From the floor farthest from the fire with no villager in the way, looking at it over the gathering
			BlockPos far = world.getServer().computeOnServer(server -> hall.floor().stream()
					.filter(spot -> server.overworld().getEntitiesOfClass(Villager.class, new AABB(spot).inflate(2)).isEmpty())
					.max(Comparator.comparingDouble(spot -> spot.distSqr(fire))).orElse(hall.floor().getFirst()));
			world.getServer().runCommand("tp @a " + (far.getX() + 0.5) + " " + (far.getY() + 0.3) + " " + (far.getZ() + 0.5) + " facing "
					+ (fire.getX() + 0.5) + " " + (fire.getY() + 0.8) + " " + (fire.getZ() + 0.5));
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			Screenshots.take(context, "tavern-evening");

			// Every tavern of the village with its fire out (a village may have an inn too), they meet at the bell as in vanilla
			int putOut = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				int fires = 0;
				for (TavernGatherings.Hall lit = TavernGatherings.find(level, meetingPoint); lit != null && fires < 8; lit = TavernGatherings.find(level, meetingPoint)) {
					level.setBlockAndUpdate(lit.fire(), level.getBlockState(lit.fire()).setValue(BlockStateProperties.LIT, false));
					fires++;
				}
				return fires;
			});
			if (world.getServer().computeOnServer(server -> TavernGatherings.find(server.overworld(), meetingPoint)) != null) {
				throw new AssertionError("Expected no gathering in a village whose taverns all have their fire out, after putting out " + putOut);
			}
			System.out.println("Tavern evening: " + putOut + " tavern fires put out");
			int left = inside;
			for (int second = 0; second < 120 && left > 0; second++) {
				context.waitTicks(20);
				left = world.getServer().computeOnServer(server -> inside(server, hall));
			}
			System.out.println("Tavern evening: " + left + " villagers left in the tavern with its fire out");
			if (left > 0) {
				throw new AssertionError("Expected the villagers to leave the tavern for the bell once its fire is out, found " + left + " still inside");
			}
			if (!world.getServer().computeOnServer(server -> server.overworld().getBlockState(meetingPoint).is(Blocks.BELL))) {
				throw new AssertionError("Expected the meeting point to stay the bell");
			}
		}
	}

	private static List<Villager> villagers(MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		return server.overworld().getEntitiesOfClass(Villager.class, new AABB(player.blockPosition()).inflate(96));
	}

	private static int inside(MinecraftServer server, TavernGatherings.Hall hall) {
		ServerLevel level = server.overworld();
		return (int) level.getEntitiesOfClass(Villager.class, AABB.of(hall.box())).stream().filter(villager -> hall.holds(level, villager)).count();
	}
}
