package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import com.cassonade.client.light.DynamicLights;
import com.cassonade.client.light.SpawnSpots;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

/**
 * Light in hand: a torch lights the dark around the player; a sculk torch marks the dark floor where
 * monsters can spawn, but not a lit block.
 */
public class LightsClientGameTest implements FabricClientGameTest {
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
			// A torch in hand lights the dark around the player, here a closed stone room deep down
			world.getServer().runCommand("execute as @a at @s run tp @s ~ -40 ~ 0 50");
			world.getServer().runCommand("execute as @a at @s run fill ~-8 ~-1 ~-8 ~8 ~5 ~8 stone hollow");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
			context.runOnClient(client -> client.gui.hud.toggle());
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			Screenshots.take(context, "cassonade-dynamic-light-off");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with torch");
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			int light = context.computeOnClient(client -> DynamicLights.light(BlockPos.containing(client.player.getEyePosition())));
			check(light == Blocks.TORCH.defaultBlockState().getLightEmission(), "the torch's light carried with the player (" + light + ")");
			Screenshots.take(context, "cassonade-dynamic-light-on");

			// A sculk torch in hand marks the dark floor where monsters can spawn, but not a lit block
			world.getServer().runCommand("execute as @a at @s run setblock ~-6 ~ ~-6 glowstone");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:sculk_torch");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 45");
			context.waitTicks(25);
			world.getConnection().waitForChunksRender();
			int spots = context.computeOnClient(client -> SpawnSpots.count());
			check(spots > 50, "the dark room's floor marked (" + spots + " spots)");
			check(context.computeOnClient(client -> SpawnSpots.isMarked(client.player.blockPosition().offset(3, 0, 3))), "the floor in front marked");
			check(!context.computeOnClient(client -> SpawnSpots.isMarked(client.player.blockPosition().offset(-5, 0, -6))), "no mark next to the glowstone");
			context.runOnClient(client -> client.gui.hud.toggle());
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-sculk-torch");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
