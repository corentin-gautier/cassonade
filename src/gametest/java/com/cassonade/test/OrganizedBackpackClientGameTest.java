package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.Recipe;

import com.cassonade.Cassonade;
import com.cassonade.gear.BackpackContainer;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.OrganizedBackpack;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;
import com.cassonade.storage.HandRefill;

public class OrganizedBackpackClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamemode survival @a");

			// At a smithing table: the upgrade keeps the contents
			world.getServer().runCommand("execute at @a run setblock ~2 ~ ~ smithing_table");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getInventory().clearContent();
				ItemStack plain = new ItemStack(CassonadeItems.BACKPACK);
				plain.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.COBBLESTONE, 10))));
				player.getInventory().setItem(9, new ItemStack(CassonadeItems.BACKPACK_UPGRADE_TEMPLATE));
				player.getInventory().setItem(10, plain);
				player.getInventory().setItem(11, new ItemStack(Items.HOPPER));
				BlockPos pos = player.blockPosition().east(2);
				player.openMenu(player.level().getBlockState(pos).getMenuProvider(player.level(), pos));
			});
			context.waitForScreen(SmithingScreen.class);
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-backpack-upgrade");
			quickMove(context, 4);
			quickMove(context, 5);
			quickMove(context, 6);
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				SmithingMenu menu = (SmithingMenu) server.getPlayerList().getPlayers().getFirst().containerMenu;
				ItemStack result = menu.getSlot(3).getItem();
				check(result.is(CassonadeItems.ORGANIZED_BACKPACK), "the organized backpack made (" + result + ")");
				check(result.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.COBBLESTONE) && stack.getCount() == 10), "its contents kept");
			});
			context.setScreen(() -> null);
			context.waitTicks(2);

			// Worn, with cobblestone, planks and an incomplete stack inside
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getInventory().clearContent();
				// Filled before it is worn, as a real one: the client learns of the backpack and of later changes separately
				ItemStack organized = new ItemStack(CassonadeItems.ORGANIZED_BACKPACK);
				organized.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
						new ItemStack(Items.COBBLESTONE, 10), new ItemStack(Items.OAK_PLANKS, 10), new ItemStack(Items.DIRT, 60))));
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.BACKPACK, organized));
				check(OrganizedBackpack.of(player) != null, "the organized backpack worn");
			});
			context.waitTicks(3);

			// Worn, it earns its advancement, which the server looks over once a second
			context.waitTicks(25);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(player.getAdvancements().getOrStartProgress(server.getAdvancements().get(Cassonade.id("organized_backpack"))).isDone(), "the advancement earned");
			});

			// Picking up: its stacks completed first, the rest in the inventory, never a new stack in it
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BackpackContainer backpack = OrganizedBackpack.of(player);
				ItemEntity dirt = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.DIRT, 10));
				dirt.setNoPickUpDelay();
				player.level().addFreshEntity(dirt);
				dirt.playerTouch(player);
				check(backpack.getItem(2).getCount() == 64, "the backpack's stack completed (" + backpack.getItem(2) + ")");
				check(player.getInventory().countItem(Items.DIRT) == 6, "the rest in the inventory");
				ItemEntity sand = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.SAND, 5));
				sand.setNoPickUpDelay();
				player.level().addFreshEntity(sand);
				sand.playerTouch(player);
				check(player.getInventory().countItem(Items.SAND) == 5, "what the backpack doesn't hold goes to the inventory");
				player.getInventory().clearContent();
			});

			// The hand: from the inventory first, then the backpack
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getInventory().setSelectedSlot(0);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE, 1));
				HandRefill.run(player, InteractionHand.MAIN_HAND, () -> player.getMainHandItem().shrink(1));
				check(player.getMainHandItem().is(Items.COBBLESTONE) && player.getMainHandItem().getCount() == 10, "the hand refilled from the backpack");
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			});
			context.waitTicks(3);

			// The recipe book: the client counts it, and placing the recipe takes it out of the backpack
			context.runOnClient(client -> {
				StackedItemContents contents = new StackedItemContents();
				client.player.getInventory().fillStackedContents(contents);
				check(contents.canCraft(client.getSingleplayerServer().getRecipeManager()
						.byKey(recipe("stick")).orElseThrow().value(), null), "the recipe craftable from the backpack, on the client");
			});
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BackpackContainer backpack = OrganizedBackpack.of(player);
				player.inventoryMenu.handlePlacement(false, true, server.getRecipeManager().byKey(recipe("stick")).orElseThrow(), player.level(), player.getInventory());
				check(backpack.getItem(1).getCount() == 8, "two planks taken for sticks (" + backpack.getItem(1) + ")");
				check(player.inventoryMenu.getSlot(0).getItem().is(Items.STICK), "the sticks in the result");
			});

			// The template in the late game's chests: at 8%, 150 rolls all without one would be under one in 100,000
			int templates = 0;
			for (int roll = 0; roll < 150 && templates == 0; roll++) {
				world.getServer().runCommand("clear @a");
				world.getServer().runCommand("loot give @a loot minecraft:chests/ancient_city");
				templates = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst()
						.getInventory().countItem(CassonadeItems.BACKPACK_UPGRADE_TEMPLATE));
			}
			check(templates > 0, "the template found in ancient city chests");

			// The plain backpack does none of it
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.BACKPACK, new ItemStack(CassonadeItems.BACKPACK)));
				check(OrganizedBackpack.of(player) == null, "the plain backpack stays plain");
			});
		}
	}

	private static ResourceKey<Recipe<?>> recipe(String name) {
		return ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(name));
	}

	private static void quickMove(ClientGameTestContext context, int slot) {
		context.runOnClient(client -> client.gameMode.handleContainerInput(client.player.containerMenu.containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player));
		context.waitTicks(1);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
