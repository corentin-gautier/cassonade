package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class LoomClientGameTest implements FabricClientGameTest {
	private static final int BANNER = 0;
	private static final int DYE = 1;
	private static final int RESULT = 3;
	private static final int INVENTORY = 4;
	private static final int PATTERN = 2;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("execute at @a run setblock ~2 ~ ~ loom");
			world.getServer().runCommand("item replace entity @a inventory.0 with white_banner 1");
			world.getServer().runCommand("item replace entity @a inventory.1 with red_dye 8");
			world.getServer().runCommand("item replace entity @a inventory.2 with blue_banner 1");
			world.getServer().runCommand("item replace entity @a inventory.3 with lime_banner 1");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.openMenu(player.level().getBlockState(player.blockPosition().east(2))
						.getMenuProvider(player.level(), player.blockPosition().east(2)));
			});
			context.waitForScreen(LoomScreen.class);
			context.waitTicks(2);

			click(context, INVENTORY);
			click(context, BANNER);
			click(context, INVENTORY + 1);
			click(context, DYE);
			context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId, PATTERN));
			context.waitTicks(2);
			checkPattern(context, world, Items.BANNER.pick(DyeColor.WHITE), "the pattern picked");

			// The banner used up, then a new stack
			click(context, RESULT);
			click(context, INVENTORY);
			check(context.computeOnClient(client -> ((LoomMenu) client.player.containerMenu).getSelectedBannerPatternIndex()) == -1, "no banner, no pattern");
			click(context, INVENTORY + 2);
			click(context, BANNER);
			checkPattern(context, world, Items.BANNER.pick(DyeColor.BLUE), "the pattern kept for a new banner");

			// A swap in a single click, the hardest case: the server's selection ends where it started
			click(context, INVENTORY + 3);
			click(context, BANNER);
			checkPattern(context, world, Items.BANNER.pick(DyeColor.LIME), "the pattern kept through a swap");
		}
	}

	private static void click(ClientGameTestContext context, int slot) {
		context.runOnClient(client -> client.gameMode.handleContainerInput(client.player.containerMenu.containerId, slot, 0, ContainerInput.PICKUP, client.player));
		context.waitTicks(2);
	}

	private static void checkPattern(ClientGameTestContext context, TestSingleplayerContext world, Item banner, String what) {
		context.runOnClient(client -> {
			LoomMenu menu = (LoomMenu) client.player.containerMenu;
			check(menu.getSelectedBannerPatternIndex() == PATTERN, what + ": selected on the client");
			checkResult(menu.getResultSlot().getItem(), banner, what + ": in the client's result slot");
		});
		world.getServer().runOnServer(server -> {
			LoomMenu menu = (LoomMenu) server.getPlayerList().getPlayers().getFirst().containerMenu;
			check(menu.getSelectedBannerPatternIndex() == PATTERN, what + ": selected on the server");
			checkResult(menu.getResultSlot().getItem(), banner, what + ": in the server's result slot");
		});
	}

	private static void checkResult(ItemStack result, Item banner, String what) {
		check(result.is(banner) && !result.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().isEmpty(), what + " (" + result + ")");
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
