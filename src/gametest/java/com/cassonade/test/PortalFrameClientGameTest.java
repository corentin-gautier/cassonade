package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** A Nether portal framed in crying obsidian, mixed with obsidian, lights. */
public class PortalFrameClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				BlockPos corner = server.getPlayerList().getPlayers().getFirst().blockPosition().above(10).east(4);
				// A 4 by 5 frame along x, its 2 by 3 inside left empty; one obsidian block among the crying ones
				for (int x = 0; x < 4; x++) {
					for (int y = 0; y < 5; y++) {
						boolean frame = x == 0 || x == 3 || y == 0 || y == 4;
						level.setBlockAndUpdate(corner.offset(x, y, 0), frame
								? (x == 0 && y == 2 ? Blocks.OBSIDIAN : Blocks.CRYING_OBSIDIAN).defaultBlockState()
								: Blocks.AIR.defaultBlockState());
					}
				}
				level.setBlockAndUpdate(corner.offset(1, 1, 0), Blocks.FIRE.defaultBlockState());
				if (!level.getBlockState(corner.offset(2, 3, 0)).is(Blocks.NETHER_PORTAL)) {
					throw new AssertionError("Expected: a frame of crying obsidian lights");
				}
			});
		}
	}
}
