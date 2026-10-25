package com.vanillasugar.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.zombie.Zombie;

import com.vanillasugar.client.combat.DamageDirection;
import com.vanillasugar.client.quest.QuestLog;
import com.vanillasugar.client.quest.QuestOptInScreen;

/**
 * Where a hit came from: a zombie on the player's left marks the left of the crosshair, and a fall
 * marks nothing.
 */
public class CombatClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			// Looking south, east is on the left
			world.getServer().runCommand("tp @a ~ ~ ~ 0 0");
			world.getServer().runCommand("execute at @a run summon zombie ~3 ~ ~ {NoAI:1b}");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Zombie zombie = player.level().getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(5)).getFirst();
				player.hurtServer(player.level(), player.damageSources().mobAttack(zombie), 1);
			});
			context.waitTicks(3);
			Float angle = context.computeOnClient(client -> DamageDirection.lastAngle());
			check(angle != null && Math.abs(angle + Mth.HALF_PI) < 0.2, "the zombie's hit marked on the left (" + angle + ")");
			context.takeScreenshot("vanillasugar-damage-direction");

			context.waitTicks(40);
			check(context.computeOnClient(client -> DamageDirection.lastAngle()) == null, "the mark gone after a moment");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.hurtServer(player.level(), player.damageSources().fall(), 1);
			});
			context.waitTicks(3);
			check(context.computeOnClient(client -> DamageDirection.lastAngle()) == null, "no mark for a fall");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
