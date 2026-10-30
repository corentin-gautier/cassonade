package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;

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
			Screenshots.take(context, "cassonade-woodcutter");
			remembersThePiece(context, world);
		}
	}

	/** Through real clicks: the client resets its own selection whenever the input changes, so it has to be told again. */
	private static void remembersThePiece(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("item replace entity @a inventory.1 with spruce_planks 8");
		context.waitTicks(2);
		int[] stairs = new int[1];
		world.getServer().runOnServer(server -> {
			StonecutterMenu menu = (StonecutterMenu) server.getPlayerList().getPlayers().getFirst().containerMenu;
			var entries = menu.getVisibleRecipes().entries();
			for (int i = 0; i < entries.size(); i++) {
				if (entries.get(i).recipe().recipe().orElseThrow().value().assemble(
						new SingleRecipeInput(menu.getSlot(0).getItem())).is(Items.OAK_STAIRS)) {
					stairs[0] = i;
				}
			}
		});
		context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId, stairs[0]));
		context.waitTicks(2);
		checkPiece(context, world, Items.OAK_STAIRS, "the piece picked");

		// A swap in a single click, the hardest case: the server's selection ends where it started
		click(context, 3);
		click(context, 0);
		context.waitTicks(2);
		checkPiece(context, world, Items.SPRUCE_STAIRS, "the same shape in another wood");

		// The slot emptied, then filled again
		click(context, 3);
		click(context, 0);
		context.waitTicks(2);
		check(context.computeOnClient(client -> ((StonecutterMenu) client.player.containerMenu).getSelectedRecipeIndex()) == -1, "an empty input selects nothing");
		click(context, 0);
		context.waitTicks(2);
		checkPiece(context, world, Items.SPRUCE_STAIRS, "the same piece for the next stack");
	}

	private static void click(ClientGameTestContext context, int slot) {
		context.runOnClient(client -> client.gameMode.handleContainerInput(client.player.containerMenu.containerId, slot, 0, ContainerInput.PICKUP, client.player));
	}

	private static void checkPiece(ClientGameTestContext context, TestSingleplayerContext world, Item expected, String what) {
		context.runOnClient(client -> {
			StonecutterMenu menu = (StonecutterMenu) client.player.containerMenu;
			check(menu.getSelectedRecipeIndex() >= 0, what + ": selected on the client");
			check(menu.getSlot(1).getItem().is(expected), what + ": in the client's result slot (" + menu.getSlot(1).getItem() + ")");
		});
		world.getServer().runOnServer(server -> {
			StonecutterMenu menu = (StonecutterMenu) server.getPlayerList().getPlayers().getFirst().containerMenu;
			check(menu.getSlot(1).getItem().is(expected), what + ": in the server's result slot (" + menu.getSlot(1).getItem() + ")");
		});
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
