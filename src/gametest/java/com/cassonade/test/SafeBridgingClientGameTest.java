package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SafeBridgingClientGameTest implements FabricClientGameTest {
	private static final BlockPos AHEAD = new BlockPos(3, 100, 2);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runCommand("fill 0 100 0 15 100 15 air");
			world.getServer().runCommand("setblock 2 100 2 stone");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cobblestone 8");
			context.getInput().holdKey(options -> options.keyShift);
			aim(context, world, 89);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(5);
			check(block(context, AHEAD).isAir(), "looking straight down, no block ahead");

			aim(context, world, 75);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(5);
			check(block(context, AHEAD).is(Blocks.COBBLESTONE), "a block in front of the one underfoot, got " + block(context, AHEAD));
			Screenshots.take(context, "cassonade-safe-bridging");

			// A torch fits on top: it goes where aimed
			world.getServer().runCommand("setblock 3 100 2 air");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with torch 8");
			aim(context, world, 75);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(5);
			check(block(context, AHEAD).isAir(), "a torch doesn't bridge");

			// Not sneaking: vanilla, nothing fits on top of the block underfoot
			context.getInput().releaseKey(options -> options.keyShift);
			world.getServer().runCommand("setblock 2 101 2 air");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cobblestone 8");
			aim(context, world, 75);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(5);
			check(block(context, AHEAD).isAir(), "standing up, no block ahead");
		}
	}

	private static void aim(ClientGameTestContext context, TestSingleplayerContext world, int pitch) {
		world.getServer().runCommand("tp @a 2.5 101 2.5 -90 " + pitch);
		context.waitTicks(10);
	}

	private static BlockState block(ClientGameTestContext context, BlockPos pos) {
		return context.computeOnClient(client -> client.level.getBlockState(pos));
	}
}
