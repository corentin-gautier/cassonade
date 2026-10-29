package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.cassonade.woodcutter.Woodcutter;

public class WoodcutterClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("execute at @a run setblock ~2 ~ ~ cassonade:woodcutter");
			world.getServer().runCommand("item replace entity @a inventory.0 with oak_planks 8");
			world.getServer().runCommand("item replace entity @a inventory.1 with stone 8");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BlockPos pos = player.blockPosition().east(2);
				check(player.level().getBlockState(pos).is(Woodcutter.BLOCK), "the woodcutter is placed");
				player.openMenu(player.level().getBlockState(pos).getMenuProvider(player.level(), pos));
				StonecutterMenu menu = (StonecutterMenu) player.containerMenu;
				check(menu.getType() == Woodcutter.MENU, "the woodcutter opens its own menu");
				menu.getSlot(0).set(new ItemStack(Items.OAK_PLANKS, 8));
				menu.slotsChanged(null);
				check(menu.getNumberOfVisibleRecipes() == 9, "a plank offers its nine pieces (" + menu.getNumberOfVisibleRecipes() + ")");
				menu.getSlot(0).set(new ItemStack(Items.STONE, 8));
				menu.slotsChanged(null);
				check(menu.getNumberOfVisibleRecipes() == 0, "the woodcutter does not cut stone");
				menu.getSlot(0).set(new ItemStack(Items.OAK_PLANKS, 8));
				menu.slotsChanged(null);
			});
			context.waitForScreen(StonecutterScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("cassonade-woodcutter");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
