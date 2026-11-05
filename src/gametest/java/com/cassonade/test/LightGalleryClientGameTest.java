package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/**
 * {@code /dev lights} builds its hall deep down: every light in a bay of its own, seen down the hall and across it. The
 * world stays in build/run/clientGameTest/saves, to be copied into run/saves and walked through.
 */
public class LightGalleryClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setName("Lights");
			state.setWorldType(state.getNormalPresetList().getFirst());
			state.setAllowCommands(true);
		}).create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ -40 ~");
			context.waitTicks(20);
			context.runOnClient(client -> client.player.connection.sendCommand("dev lights"));
			context.waitTicks(40);
			boolean built = world.getServer().computeOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				BlockPos origin = player.blockPosition();
				return player.level().getBlockState(origin.offset(4, 0, -6)).is(Blocks.TORCH)
						&& player.level().getBlockState(origin.offset(4, 0, 6)).is(Blocks.LANTERN)
						&& player.level().getBlockState(origin.offset(4, 5, 0)).is(Blocks.STONE);
			});
			if (!built) {
				throw new AssertionError("/dev lights builds its hall around the player, a torch and a lantern in the first bays");
			}
			context.runOnClient(client -> client.gui.hud.toggle());
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "light-gallery-hall");
			world.getServer().runCommand("execute as @a at @s run tp @s ~44 ~ ~3 180 10");
			context.waitTicks(10);
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "light-gallery-north");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~-6 0 10");
			context.waitTicks(10);
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "light-gallery-south");
			// The sculk sensors' bays, whose light hardly leaves the block
			world.getServer().runCommand("execute as @a at @s run tp @s ~28 ~ ~6 0 25");
			context.waitTicks(10);
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "light-gallery-sculk");
			// Set active, as a vibration does for a moment: the tendrils glow
			world.getServer().runCommand("execute as @a at @s run setblock ~ ~ ~3 calibrated_sculk_sensor[sculk_sensor_phase=active]");
			world.getServer().runCommand("execute as @a at @s run setblock ~ ~ ~-9 sculk_sensor[sculk_sensor_phase=active]");
			context.waitTicks(5);
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "light-gallery-sculk-active");
			// Back at the hall's start, to open the world there
			world.getServer().runCommand("execute as @a at @s run tp @s ~-72 ~ ~-3 -90 0");
			context.runOnClient(client -> client.gui.hud.toggle());
		}
	}
}
