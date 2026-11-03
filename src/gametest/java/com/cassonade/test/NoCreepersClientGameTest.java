package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnPlacements;

import com.cassonade.tweaks.NoCreepers;

/** With cassonade:spawn_creepers off, creepers no longer spawn naturally; zombies still do, and a spawn egg still gives a creeper. */
public class NoCreepersClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("difficulty normal");
			// A closed stone room, dark whatever the time, its light settled before the checks
			world.getServer().runCommand("fill -2 199 -2 2 203 2 stone hollow");
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				BlockPos pos = new BlockPos(0, 200, 0);
				check(level.getGameRules().get(NoCreepers.SPAWN_CREEPERS), "the rule is on by default");
				check(spawns(level, EntityTypes.CREEPER, EntitySpawnReason.NATURAL, pos), "creepers spawn by default");
			});
			world.getServer().runCommand("gamerule cassonade:spawn_creepers false");
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				BlockPos pos = new BlockPos(0, 200, 0);
				check(!level.getGameRules().get(NoCreepers.SPAWN_CREEPERS), "the command turns the rule off");
				check(!spawns(level, EntityTypes.CREEPER, EntitySpawnReason.NATURAL, pos), "no natural creeper");
				check(!spawns(level, EntityTypes.CREEPER, EntitySpawnReason.CHUNK_GENERATION, pos), "no creeper with new chunks");
				check(spawns(level, EntityTypes.ZOMBIE, EntitySpawnReason.NATURAL, pos), "zombies still spawn");
				check(spawns(level, EntityTypes.CREEPER, EntitySpawnReason.SPAWN_ITEM_USE, pos), "a spawn egg still works");
			});
		}
	}

	private static boolean spawns(ServerLevel level, EntityType<?> type, EntitySpawnReason reason, BlockPos pos) {
		// The light test is random: a few tries
		for (int i = 0; i < 20; i++) {
			if (SpawnPlacements.checkSpawnRules(type, level, reason, pos, level.getRandom())) {
				return true;
			}
		}
		return false;
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
