package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;
import static com.cassonade.test.PlannedWorld.player;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

public class ChainCraftingClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runCommand("recipe give @a *");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.getInventory().clearContent();
				player.getInventory().add(new ItemStack(Items.OAK_LOG, 2));

				// Two logs make the eight planks of a chest
				CraftingMenu menu = new CraftingMenu(100, player.getInventory(), ContainerLevelAccess.NULL);
				place(menu, player, server, "chest");
				check(count(player, Items.OAK_LOG) == 0, "the logs used");
				check(inGrid(menu, Items.OAK_PLANKS) == 8, "the planks in the grid: " + inGrid(menu, Items.OAK_PLANKS));

				// One log is not enough: nothing used up
				clear(menu, player);
				player.getInventory().clearContent();
				player.getInventory().add(new ItemStack(Items.OAK_LOG, 1));
				place(menu, player, server, "chest");
				check(count(player, Items.OAK_LOG) == 1 && count(player, Items.OAK_PLANKS) == 0, "out of reach, the log stays");

				// Two steps: log, planks, sticks for a wooden shovel; the rest stays in the inventory
				player.getInventory().clearContent();
				player.getInventory().add(new ItemStack(Items.OAK_LOG, 1));
				player.getInventory().add(new ItemStack(Items.STONE, 1));
				player.getInventory().add(new ItemStack(Items.COBBLESTONE, 1));
				place(menu, player, server, "stone_shovel");
				check(inGrid(menu, Items.COBBLESTONE) + inGrid(menu, Items.STONE) == 1 && inGrid(menu, Items.STICK) == 2, "a head and two sticks in the grid");
				check(count(player, Items.OAK_LOG) == 0 && count(player, Items.OAK_PLANKS) > 0, "the planks left over kept");

				// A recipe the player doesn't know is no step
				clear(menu, player);
				player.getInventory().clearContent();
				player.getInventory().add(new ItemStack(Items.OAK_LOG, 2));
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "recipe take @a minecraft:oak_planks");
				place(menu, player, server, "chest");
				check(count(player, Items.OAK_LOG) == 2, "planks unknown, the logs stay");

				// With all it takes in hand, nothing changes from vanilla
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "recipe give @a minecraft:oak_planks");
				player.getInventory().clearContent();
				player.getInventory().add(new ItemStack(Items.OAK_LOG, 1));
				player.getInventory().add(new ItemStack(Items.OAK_PLANKS, 8));
				place(menu, player, server, "chest");
				check(count(player, Items.OAK_LOG) == 1, "the log not touched");
			});
		}
	}

	private static void place(CraftingMenu menu, ServerPlayer player, MinecraftServer server, String recipe) {
		RecipeHolder<?> holder = server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(recipe))).orElseThrow();
		menu.handlePlacement(false, false, holder, player.level(), player.getInventory());
	}

	private static void clear(CraftingMenu menu, ServerPlayer player) {
		menu.getInputGridSlots().forEach(slot -> slot.set(ItemStack.EMPTY));
	}

	private static int inGrid(CraftingMenu menu, Item item) {
		int total = 0;
		for (Slot slot : menu.getInputGridSlots()) {
			total += slot.getItem().is(item) ? slot.getItem().getCount() : 0;
		}
		return total;
	}

	private static int count(ServerPlayer player, Item item) {
		return player.getInventory().countItem(item);
	}
}
